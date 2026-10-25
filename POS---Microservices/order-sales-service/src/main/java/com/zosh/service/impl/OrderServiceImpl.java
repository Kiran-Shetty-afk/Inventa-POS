package com.zosh.service.impl;

import com.zosh.client.InventoryClient;
import com.zosh.client.FraudDetectionClient;
import com.zosh.client.UserOrgClient;
import com.zosh.domain.OrderStatus;
import com.zosh.domain.PaymentType;
import com.zosh.domain.PaymentSettlementStatus;
import com.zosh.exception.UserException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.zosh.mapper.OrderMapper;
import com.zosh.modal.Customer;
import com.zosh.modal.Order;
import com.zosh.modal.OrderItem;
import com.zosh.payload.dto.BranchSummaryDTO;
import com.zosh.payload.dto.CurrentUserDTO;
import com.zosh.payload.dto.FraudPredictionRequest;
import com.zosh.payload.dto.FraudPredictionResponse;
import com.zosh.payload.dto.OrderDTO;
import com.zosh.payload.dto.OrderItemDTO;
import com.zosh.payload.dto.ProductDTO;
import com.zosh.repository.CustomerRepository;
import com.zosh.repository.OrderRepository;
import com.zosh.service.OrderService;
import com.zosh.service.OrderWorkflowCoordinator;
import com.zosh.service.OrderWorkflowProcessor;
import com.zosh.service.OrderPricingPolicy;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final int CURRENCY_SCALE = 2;
    private static final BigDecimal ZERO_MONEY = new BigDecimal("0.00");

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final InventoryClient inventoryClient;
    private final FraudDetectionClient fraudDetectionClient;
    private final UserOrgClient userOrgClient;
    private final OrderWorkflowCoordinator orderWorkflowCoordinator;
    private final OrderWorkflowProcessor orderWorkflowProcessor;
    private final OrderPricingPolicy orderPricingPolicy;

    @Override
    public OrderDTO createOrder(OrderDTO dto) throws UserException {
        return createOrder(dto, UUID.randomUUID().toString());
    }

    @Override
    public OrderDTO createOrder(OrderDTO dto, String idempotencyKey) throws UserException {
        if (dto == null || dto.getBranchId() == null || dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new IllegalArgumentException("Branch and at least one order item are required");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> allowedRoles = Set.of("ROLE_BRANCH_CASHIER", "ROLE_CASHIER");
        boolean canCreateOrder = authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(allowedRoles::contains);
        if (!canCreateOrder) {
            throw new AccessDeniedException("A cashier account is required to create an order");
        }

        Long branchId = dto.getBranchId();
        Object branchClaim = authentication.getDetails();
        if (branchClaim instanceof Number number && !Objects.equals(number.longValue(), branchId)) {
            throw new AccessDeniedException("Orders can only be created for the assigned branch");
        }
        if (!(branchClaim instanceof Number)) {
            throw new AccessDeniedException("The account is not assigned to a branch");
        }

        BranchSummaryDTO branch = userOrgClient.getBranch(branchId);
        if (branch == null || branch.storeId() == null
                || !Boolean.TRUE.equals(userOrgClient.canCurrentUserAccessBranch(
                        branchId, branch.storeId(), "ORDER_CREATE"))) {
            throw new AccessDeniedException("You are not authorized to create orders for this branch");
        }
        CurrentUserDTO currentUser = userOrgClient.getCurrentUserProfile();
        if (currentUser == null || currentUser.id() == null) {
            throw new AccessDeniedException("The current user profile could not be verified");
        }

        if (idempotencyKey == null || !idempotencyKey.matches("[A-Fa-f0-9]{8}-(?:[A-Fa-f0-9]{4}-){3}[A-Fa-f0-9]{12}")) {
            throw new IllegalArgumentException("A UUID idempotency key is required");
        }
        var existingRequest = orderRepository.findByIdempotencyKey(idempotencyKey);
        if (existingRequest.isPresent()) {
            Order existing = existingRequest.get();
            if (!Objects.equals(existing.getBranchId(), branchId)
                    || !Objects.equals(existing.getCashierId(), currentUser.id())) {
                throw new AccessDeniedException("This idempotency key belongs to another order");
            }
            if (existing.getStatus() == OrderStatus.PENDING) {
                try {
                    existing = orderWorkflowProcessor.resumeOrder(existing.getId());
                } catch (RuntimeException exception) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Order processing is still pending. Retry with the same Idempotency-Key.");
                }
            }
            if (existing.getStatus() == OrderStatus.FAILED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Order could not be completed because inventory was unavailable");
            }
            return OrderMapper.toDto(existing);
        }

        Customer customer = resolveCustomer(dto.getCustomer(), branchId, branch.storeId());
        Order order = Order.builder()
                .idempotencyKey(idempotencyKey)
                .branchId(branchId)
                .cashierId(currentUser.id())
                .customer(customer)
                .paymentType(dto.getPaymentType() == null ? PaymentType.CASH : dto.getPaymentType())
                .paymentSettlementStatus(dto.getPaymentType() == null || dto.getPaymentType() == PaymentType.CASH
                        ? PaymentSettlementStatus.CASH_RECORDED
                        : PaymentSettlementStatus.EXTERNAL_CONFIRMATION_REQUIRED)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        Map<Long, Integer> requestedStock = new LinkedHashMap<>();
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = ZERO_MONEY;
        int totalQuantity = 0;

        for (OrderItemDTO itemDto : dto.getItems()) {
            if (itemDto == null || itemDto.getProductId() == null
                    || itemDto.getQuantity() == null || itemDto.getQuantity() <= 0) {
                throw new IllegalArgumentException("Each order item needs a product and positive quantity");
            }

            ProductDTO product = inventoryClient.getProductById(itemDto.getProductId());
            if (product == null || !Objects.equals(product.getId(), itemDto.getProductId())
                    || product.getSellingPrice() == null
                    || product.getSellingPrice().signum() <= 0) {
                throw new IllegalArgumentException("A valid catalog price is required for every order item");
            }
            if (product.getStoreId() == null
                    || !Boolean.TRUE.equals(userOrgClient.canCurrentUserAccessBranch(
                            branchId, product.getStoreId(), "ORDER_CREATE"))) {
                throw new AccessDeniedException("The selected product does not belong to this branch's store");
            }

            int quantity = itemDto.getQuantity();
            int aggregatedQuantity = Math.addExact(requestedStock.getOrDefault(product.getId(), 0), quantity);
            requestedStock.put(product.getId(), aggregatedQuantity);

            BigDecimal unitPrice = requireCurrencyAmount(product.getSellingPrice(), "Catalog price");
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity))
                    .setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
            requireSupportedMoney(lineTotal, "Order item total");
            subtotal = subtotal.add(lineTotal).setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
            requireSupportedMoney(subtotal, "Order subtotal");
            totalQuantity = Math.addExact(totalQuantity, quantity);
            orderItems.add(OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .productImage(product.getImage())
                    .productPrice(unitPrice)
                    .quantity(quantity)
                    .price(lineTotal)
                    .order(order)
                    .build());
        }

        double requestedPercentValue = dto.getDiscountPercent() == null ? 0.0 : dto.getDiscountPercent();
        if (!Double.isFinite(requestedPercentValue)) {
            throw new IllegalArgumentException("Discount percentage must be a finite number");
        }
        BigDecimal requestedPercent = BigDecimal.valueOf(requestedPercentValue);
        BigDecimal requestedAmount = requireCurrencyAmount(dto.getDiscountAmount(), "Discount amount");
        if (requestedAmount.signum() != 0) {
            throw new IllegalArgumentException("Submit a discount percentage; the server calculates the discount amount");
        }
        BigDecimal discountAmount = orderPricingPolicy.calculateDiscountAmount(
                subtotal, requestedPercent, branch.maxDiscountPercent());
        BigDecimal taxAmount = orderPricingPolicy.calculateTax(subtotal);
        BigDecimal total = subtotal.add(taxAmount).subtract(discountAmount)
                .setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        requireSupportedMoney(total, "Order total");

        order.setItems(orderItems);
        order.setItemsCount(orderItems.size());
        order.setTotalQuantity(totalQuantity);
        order.setSubtotalAmount(subtotal);
        order.setTaxRate(orderPricingPolicy.taxRatePercent());
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(total);
        order.setDiscountAmount(discountAmount);
        order.setDiscountPercent(requestedPercent.setScale(2, RoundingMode.UNNECESSARY).doubleValue());
        order.setShift(getShift(order.getCreatedAt()));
        order.setDayOfWeek(order.getCreatedAt().getDayOfWeek().toString());
        order.setOrderHour(order.getCreatedAt().getHour());
        order.setAverageItemPrice(totalQuantity > 0
                ? subtotal.divide(BigDecimal.valueOf(totalQuantity), 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
        order.setRiskScore(0.0);
        order.setIsFraud(false);
        order.setFraudStatus("NONE");
        scoreForFraud(order);

        Long workflowId = orderWorkflowCoordinator.startPending(order, requestedStock);
        try {
            Order processed = orderWorkflowProcessor.processOrder(workflowId);
            if (processed.getStatus() == OrderStatus.FAILED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Order could not be completed because inventory was unavailable");
            }
            return OrderMapper.toDto(processed);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Order workflow {} will be resumed using its persisted idempotency key", workflowId);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Order processing is pending. Retry with the same Idempotency-Key.");
        }
    }
    private void scoreForFraud(Order order) {
        try {
            FraudPredictionResponse prediction = fraudDetectionClient.predictFraud(
                    new FraudPredictionRequest(
                            order.getTotalAmount() != null ? order.getTotalAmount().doubleValue() : 0.0,
                            order.getPaymentType() != null ? order.getPaymentType().name() : PaymentType.CASH.name(),
                            order.getItemsCount() != null ? order.getItemsCount() : 0,
                            order.getTotalQuantity() != null ? order.getTotalQuantity() : 0,
                            order.getAverageItemPrice() != null ? order.getAverageItemPrice().doubleValue() : 0.0,
                            order.getDiscountPercent() != null ? order.getDiscountPercent() : 0.0,
                            order.getOrderHour() != null ? order.getOrderHour() : 0));

            if (prediction == null || prediction.fraudProbability() == null
                    || !Double.isFinite(prediction.fraudProbability())) {
                log.warn("ML fraud scoring returned an invalid response; saving order without a fraud alert");
                return;
            }

            double probability = Math.max(0.0, Math.min(1.0, prediction.fraudProbability()));
            double riskScore = Math.round(probability * 1000.0) / 10.0;
            boolean flagged = Integer.valueOf(1).equals(prediction.prediction());

            order.setRiskScore(riskScore);
            order.setIsFraud(flagged);
            if (flagged) {
                order.setFraudStatus("OPEN");
                order.setFraudReason(String.format(Locale.ROOT,
                        "AI model flagged this order; AI Probability: %.1f%%", riskScore));
            }
        } catch (RuntimeException e) {
            log.warn("ML fraud scoring is unavailable; saving order without a fraud alert");
        }
    }

    // ================================================================
    // GET ORDER BY ID
    // ================================================================

    @Override
    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
        requireBranchAccess(order.getBranchId(), "READ");
        return OrderMapper.toDto(order);
    }

    // ================================================================
    // GET ORDERS BY BRANCH
    // ================================================================

    @Override
    public List<OrderDTO> getOrdersByBranch(
            Long branchId,
            Long customerId,
            Long cashierId,
            PaymentType paymentType,
            OrderStatus status) {

        requireBranchAccess(branchId, "READ");
        Long scopedCashierId = isCashier() ? currentUserId() : cashierId;

        return orderRepository
                .findByBranchId(branchId)
                .stream()

                .filter(order -> customerId == null ||
                        (order.getCustomer() != null &&
                                order.getCustomer()
                                        .getId()
                                        .equals(customerId)))

                .filter(order -> scopedCashierId == null ||
                        (order.getCashierId() != null &&
                                order.getCashierId()
                                        .equals(scopedCashierId)))

                .filter(order -> paymentType == null ||
                        order.getPaymentType() == paymentType)

                .filter(order -> status == null ||
                        order.getStatus() == status)

                .map(OrderMapper::toDto)

                .sorted((o1, o2) -> {

                    if (o1.getCreatedAt() == null ||
                            o2.getCreatedAt() == null) {
                        return 0;
                    }

                    return o2.getCreatedAt()
                            .compareTo(o1.getCreatedAt());
                })

                .collect(Collectors.toList());
    }

    // ================================================================
    // GET ORDERS BY CASHIER
    // ================================================================

    @Override
    public List<OrderDTO> getOrdersByCashier(
            Long cashierId) {
        if (isCashier() && !Objects.equals(currentUserId(), cashierId)) {
            throw new AccessDeniedException("Cashiers can only access their own orders");
        }
        return orderRepository
                .findByCashierId(cashierId)
                .stream()
                .filter(order -> canAccessBranch(order.getBranchId(), "READ"))
                .map(OrderMapper::toDto)
                .collect(Collectors.toList());
    }

    // ================================================================
    // DELETE ORDER
    // ================================================================

    @Override
    public void deleteOrder(Long id) {
        cancelOrder(id);
    }

    @Override
    public OrderDTO cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
        requireBranchAccess(order.getBranchId(), "ORDER_MANAGE");
        try {
            Order cancelled = orderWorkflowProcessor.cancelOrder(id);
            if (cancelled.getStatus() == OrderStatus.FAILED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cancellation needs manual stock reconciliation");
            }
            return OrderMapper.toDto(cancelled);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Cancellation is pending and will be safely retried");
        }
    }

    // ================================================================
    // TODAY'S ORDERS
    // ================================================================

    @Override
    public List<OrderDTO> getTodayOrdersByBranch(
            Long branchId) {

        requireBranchAccess(branchId, "READ");

        LocalDate today = LocalDate.now();

        LocalDateTime start = today.atStartOfDay();

        LocalDateTime end = today.plusDays(1).atStartOfDay();

        return orderRepository
                .findByBranchIdAndCreatedAtBetween(
                        branchId,
                        start,
                        end)

                .stream()
                .map(OrderMapper::toDto)
                .collect(Collectors.toList());
    }

    // ================================================================
    // GET ORDERS BY CUSTOMER
    // ================================================================

    @Override
    public List<OrderDTO> getOrdersByCustomerId(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found with id: " + customerId));
        List<Order> customerOrders = orderRepository.findByCustomerId(customerId);
        boolean canAccessCustomer = canAccessCustomer(customer);

        List<Order> accessibleOrders = customerOrders.stream()
                .filter(order -> order.getBranchId() == null
                        ? canAccessCustomer
                        : isPlatformAdmin() || canAccessBranch(order.getBranchId(), "READ"))
                .toList();
        if (!canAccessCustomer && accessibleOrders.isEmpty()) {
            throw new AccessDeniedException("You are not authorized to view this customer's purchase history");
        }

        return accessibleOrders.stream()
                .map(OrderMapper::toDto)
                .collect(Collectors.toList());
    }

    // ================================================================
    // GET TOP 5 RECENT ORDERS
    // ================================================================

    @Override
    public List<OrderDTO> getTop5RecentOrdersByBranchId(
            Long branchId) {

        requireBranchAccess(branchId, "READ");

        List<Order> orders = orderRepository
                .findTop5ByBranchIdOrderByCreatedAtDesc(
                        branchId);

        return orders
                .stream()
                .map(OrderMapper::toDto)
                .collect(Collectors.toList());
    }

    // ================================================================
    // RESOLVE CUSTOMER
    // ================================================================

    private Customer resolveCustomer(Customer requestedCustomer, Long branchId, Long storeId) {
        if (requestedCustomer == null) {
            return null;
        }

        if (requestedCustomer.getId() != null) {
            Customer existing = customerRepository.findById(requestedCustomer.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
            boolean sameBranch = Objects.equals(existing.getBranchId(), branchId);
            boolean storeWideCustomer = existing.getBranchId() == null
                    && Objects.equals(existing.getStoreId(), storeId);
            if (!sameBranch && !storeWideCustomer) {
                throw new AccessDeniedException("The customer does not belong to this branch or store");
            }
            return existing;
        }

        if (requestedCustomer.getFullName() == null || requestedCustomer.getFullName().isBlank()) {
            throw new IllegalArgumentException("Customer name is required");
        }
        return Customer.builder()
                .fullName(requestedCustomer.getFullName().trim())
                .email(requestedCustomer.getEmail())
                .phone(requestedCustomer.getPhone())
                .storeId(storeId)
                .branchId(branchId)
                .loyaltyPoints(0)
                .build();
    }

    private void requireBranchAccess(Long branchId, String permission) {
        if (!canAccessBranch(branchId, permission)) {
            throw new AccessDeniedException("You are not authorized to access orders for this branch");
        }
    }

    private boolean canAccessBranch(Long branchId, String permission) {
        return branchId != null && Boolean.TRUE.equals(
                userOrgClient.canCurrentUserAccessBranch(branchId, null, permission));
    }

    private boolean canAccessCustomer(Customer customer) {
        if (isPlatformAdmin()) {
            return true;
        }
        if (customer.getBranchId() != null) {
            return Boolean.TRUE.equals(userOrgClient.canCurrentUserAccessBranch(
                    customer.getBranchId(), customer.getStoreId(), "READ"));
        }
        return customer.getStoreId() != null
                && Boolean.TRUE.equals(userOrgClient.canCurrentUserAccessStore(customer.getStoreId()));
    }

    private boolean isPlatformAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private boolean isCashier() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_BRANCH_CASHIER".equals(role) || "ROLE_CASHIER".equals(role));
    }

    private Long currentUserId() {
        CurrentUserDTO currentUser = userOrgClient.getCurrentUserProfile();
        if (currentUser == null || currentUser.id() == null) {
            throw new AccessDeniedException("The current user profile could not be verified");
        }
        return currentUser.id();
    }

    // ================================================================
    // LOYALTY POINTS
    // ================================================================

    private int calculatePointsEarned(BigDecimal totalAmount) {
        if (totalAmount == null || totalAmount.signum() <= 0) {
            return 0;
        }
        BigDecimal points = totalAmount.divideToIntegralValue(BigDecimal.valueOf(100));
        return points.min(BigDecimal.valueOf(Integer.MAX_VALUE)).intValue();
    }

    private BigDecimal requireCurrencyAmount(BigDecimal amount, String fieldName) {
        if (amount == null) {
            return ZERO_MONEY;
        }
        BigDecimal rounded = amount.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        if (amount.compareTo(rounded) != 0) {
            throw new IllegalArgumentException(fieldName + " must have at most two decimal places");
        }
        requireSupportedMoney(rounded, fieldName);
        return rounded;
    }

    private void requireSupportedMoney(BigDecimal amount, String fieldName) {
        BigDecimal maximum = new BigDecimal("99999999999999999.99");
        if (amount == null || amount.abs().compareTo(maximum) > 0) {
            throw new IllegalArgumentException(fieldName + " is outside the supported currency range");
        }
    }

    private int currentLoyaltyPoints(
            Customer customer) {

        return customer.getLoyaltyPoints() == null
                ? 0
                : customer.getLoyaltyPoints();
    }

    // ================================================================
    // SHIFT
    // ================================================================

    private String getShift(
            LocalDateTime time) {

        int hour = time.getHour();

        if (hour >= 6 && hour < 12) {
            return "Morning";
        }

        if (hour >= 12 && hour < 17) {
            return "Afternoon";
        }

        if (hour >= 17 && hour < 22) {
            return "Evening";
        }

        return "Night";
    }
}
