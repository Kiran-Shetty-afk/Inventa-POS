package com.zosh.service.impl;

import com.zosh.client.InventoryClient;
import com.zosh.domain.OrderStatus;
import com.zosh.domain.PaymentType;
import com.zosh.exception.UserException;
import com.zosh.mapper.OrderMapper;
import com.zosh.modal.Customer;
import com.zosh.modal.Order;
import com.zosh.modal.OrderItem;
import com.zosh.payload.dto.InventoryDTO;
import com.zosh.payload.dto.OrderDTO;
import com.zosh.payload.dto.OrderItemDTO;
import com.zosh.payload.dto.ProductDTO;
import com.zosh.repository.CustomerRepository;
import com.zosh.repository.OrderRepository;
import com.zosh.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final InventoryClient inventoryClient;

    @Override
    @Transactional
    public OrderDTO createOrder(OrderDTO dto) throws UserException {

        Long branchId = dto.getBranchId();
        Long cashierId = dto.getCashierId();

        log.info("Processing order creation for Branch ID: {}, Cashier ID: {}, Items count: {}", 
                branchId, cashierId, dto.getItems() != null ? dto.getItems().size() : 0);

        Customer customer = resolveCustomer(dto.getCustomer());

        Order order = Order.builder()
                .branchId(branchId)
                .cashierId(cashierId)
                .customer(customer)
                .paymentType(
                        dto.getPaymentType() != null
                                ? dto.getPaymentType()
                                : PaymentType.CASH)
                .status(
                        dto.getStatus() != null
                                ? dto.getStatus()
                                : OrderStatus.COMPLETED)
                .createdAt(
                        dto.getCreatedAt() != null
                                ? dto.getCreatedAt()
                                : LocalDateTime.now())
                .build();

        // ============================================================
        // STEP 13 + STEP 14
        // CHECK INVENTORY AND REDUCE INVENTORY
        // ============================================================

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {

            for (OrderItemDTO itemDto : dto.getItems()) {

                Long productId = itemDto.getProductId();

                int requestedQty = itemDto.getQuantity() != null
                        ? itemDto.getQuantity()
                        : 1;

                // Product ID validation
                if (productId == null) {
                    throw new IllegalArgumentException(
                            "Product ID is required");
                }

                // Quantity validation
                if (requestedQty <= 0) {
                    throw new IllegalArgumentException(
                            "Quantity must be greater than zero");
                }

                // ----------------------------------------------------
                // GET INVENTORY FROM INVENTORY SERVICE
                // ----------------------------------------------------

                log.info("Fetching inventory for Branch ID: {} and Product ID: {}", branchId, productId);
                InventoryDTO inventory = inventoryClient.getInventoryByBranchAndProduct(
                        branchId,
                        productId);

                // Inventory does not exist
                if (inventory == null) {
                    log.error("Inventory not found for branch {} and product {}", branchId, productId);
                    throw new EntityNotFoundException(
                            "Inventory not found for branch "
                                    + branchId
                                    + " and product "
                                    + productId);
                }

                Integer availableQty = inventory.getQuantity() != null ? inventory.getQuantity() : 0;

                // ----------------------------------------------------
                // CHECK STOCK
                // ----------------------------------------------------

                if (availableQty < requestedQty) {
                    log.warn("Insufficient inventory for product {}. Available: {}, Requested: {}", 
                            productId, availableQty, requestedQty);
                    throw new IllegalArgumentException(
                            "Insufficient inventory for product "
                                    + productId
                                    + ". Available: "
                                    + availableQty
                                    + ", requested: "
                                    + requestedQty);
                }

                // ----------------------------------------------------
                // REDUCE STOCK
                // ----------------------------------------------------

                int newQuantity = availableQty - requestedQty;
                inventory.setQuantity(newQuantity);

                log.info("Updating Inventory via Feign - ID: {}, Product: {}, Old Quantity: {}, Requested: {}, New Quantity: {}", 
                        inventory.getId(), productId, availableQty, requestedQty, newQuantity);

                InventoryDTO updatedInventory = inventoryClient.updateInventory(
                        inventory.getId(),
                        inventory);

                log.info("Inventory successfully updated via Feign for ID {}: result quantity = {}", 
                        inventory.getId(), updatedInventory != null ? updatedInventory.getQuantity() : newQuantity);
            }
        }

        // ============================================================
        // CREATE ORDER ITEMS
        // ============================================================

        List<OrderItem> orderItems = new ArrayList<>();

        if (dto.getItems() != null && !dto.getItems().isEmpty()) {

            for (OrderItemDTO itemDto : dto.getItems()) {

                int requestedQty = itemDto.getQuantity() != null
                        ? itemDto.getQuantity()
                        : 1;

                double unitPrice = 0.0;

                String productName = "";

                String productSku = "";

                String productImage = "";

                // Product information supplied in request or fetched from inventory-catalog-service
                ProductDTO product = itemDto.getProduct();
                if ((product == null || product.getSellingPrice() == null || product.getSellingPrice() == 0.0) && itemDto.getProductId() != null) {
                    try {
                        ProductDTO fetchedProduct = inventoryClient.getProductById(itemDto.getProductId());
                        if (fetchedProduct != null) {
                            product = fetchedProduct;
                        }
                    } catch (Exception e) {
                        log.warn("Could not fetch product details via Feign for product ID {}: {}", itemDto.getProductId(), e.getMessage());
                    }
                }

                if (product != null) {
                    unitPrice = product.getSellingPrice() != null
                            ? product.getSellingPrice()
                            : (product.getMrp() != null ? product.getMrp() : 0.0);
                    productName = product.getName() != null ? product.getName() : "";
                    productSku = product.getSku() != null ? product.getSku() : "";
                    productImage = product.getImage() != null ? product.getImage() : "";
                } else if (itemDto.getPrice() != null) {
                    unitPrice = itemDto.getPrice() / requestedQty;
                }

                double lineTotal = (itemDto.getPrice() != null && itemDto.getPrice() > 0 && product == null)
                        ? itemDto.getPrice()
                        : unitPrice * requestedQty;

                OrderItem orderItem = OrderItem.builder()
                        .productId(itemDto.getProductId())
                        .productName(productName)
                        .productSku(productSku)
                        .productImage(productImage)
                        .productPrice(unitPrice)
                        .quantity(requestedQty)
                        .price(lineTotal)
                        .order(order)
                        .build();

                orderItems.add(orderItem);
            }
        }

        // ============================================================
        // CALCULATE ORDER TOTAL
        // ============================================================

        double total = orderItems.stream()
                .mapToDouble(OrderItem::getPrice)
                .sum();

        order.setTotalAmount(total);

        order.setItems(orderItems);

        order.setItemsCount(orderItems.size());

        // ============================================================
        // CALCULATE TOTAL QUANTITY
        // ============================================================

        int totalQuantity = orderItems.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        order.setTotalQuantity(totalQuantity);

        // ============================================================
        // DISCOUNT
        // ============================================================

        order.setDiscountAmount(
                dto.getDiscountAmount() != null
                        ? dto.getDiscountAmount()
                        : 0.0);

        order.setDiscountPercent(
                dto.getDiscountPercent() != null
                        ? dto.getDiscountPercent()
                        : 0.0);

        // ============================================================
        // ORDER ANALYTICS DATA
        // ============================================================

        LocalDateTime orderTime = order.getCreatedAt();

        order.setShift(
                getShift(orderTime));

        order.setDayOfWeek(
                orderTime.getDayOfWeek().toString());

        order.setOrderHour(
                orderTime.getHour());

        order.setAverageItemPrice(
                totalQuantity > 0
                        ? total / totalQuantity
                        : 0.0);

        // ============================================================
        // FRAUD DEFAULT VALUES
        // ============================================================

        order.setRiskScore(0.0);

        order.setIsFraud(false);

        // ============================================================
        // CUSTOMER LOYALTY POINTS
        // ============================================================

        if (customer != null) {

            customer.setLoyaltyPoints(
                    currentLoyaltyPoints(customer)
                            + calculatePointsEarned(total));

            customerRepository.save(customer);
        }

        // ============================================================
        // SAVE ORDER
        // ============================================================

        Order savedOrder = orderRepository.save(order);

        return OrderMapper.toDto(savedOrder);
    }

    // ================================================================
    // GET ORDER BY ID
    // ================================================================

    @Override
    public OrderDTO getOrderById(Long id) {

        return orderRepository.findById(id)
                .map(OrderMapper::toDto)
                .orElseThrow(
                        () -> new EntityNotFoundException(
                                "Order not found with id: " + id));
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

        return orderRepository
                .findByBranchId(branchId)
                .stream()

                .filter(order -> customerId == null ||
                        (order.getCustomer() != null &&
                                order.getCustomer()
                                        .getId()
                                        .equals(customerId)))

                .filter(order -> cashierId == null ||
                        (order.getCashierId() != null &&
                                order.getCashierId()
                                        .equals(cashierId)))

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

        return orderRepository
                .findByCashierId(cashierId)
                .stream()
                .map(OrderMapper::toDto)
                .collect(Collectors.toList());
    }

    // ================================================================
    // DELETE ORDER
    // ================================================================

    @Override
    public void deleteOrder(Long id) {

        if (!orderRepository.existsById(id)) {

            throw new EntityNotFoundException(
                    "Order not found with id: " + id);
        }

        orderRepository.deleteById(id);
    }

    // ================================================================
    // TODAY'S ORDERS
    // ================================================================

    @Override
    public List<OrderDTO> getTodayOrdersByBranch(
            Long branchId) {

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
    public List<OrderDTO> getOrdersByCustomerId(
            Long customerId) {

        return orderRepository
                .findByCustomerId(customerId)
                .stream()
                .map(OrderMapper::toDto)
                .collect(Collectors.toList());
    }

    // ================================================================
    // GET TOP 5 RECENT ORDERS
    // ================================================================

    @Override
    public List<OrderDTO> getTop5RecentOrdersByBranchId(
            Long branchId) {

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

    private Customer resolveCustomer(
            Customer requestedCustomer) {

        if (requestedCustomer == null) {
            return null;
        }

        if (requestedCustomer.getId() != null) {

            return customerRepository
                    .findById(requestedCustomer.getId())
                    .orElse(requestedCustomer);
        }

        return customerRepository.save(
                requestedCustomer);
    }

    // ================================================================
    // LOYALTY POINTS
    // ================================================================

    private int calculatePointsEarned(
            double totalAmount) {

        if (totalAmount <= 0) {
            return 0;
        }

        return Math.max(
                1,
                (int) Math.floor(
                        totalAmount / 100.0));
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