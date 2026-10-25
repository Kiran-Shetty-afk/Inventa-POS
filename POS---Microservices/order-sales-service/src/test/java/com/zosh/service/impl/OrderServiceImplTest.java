package com.zosh.service.impl;

import com.zosh.client.FraudDetectionClient;
import com.zosh.client.InventoryClient;
import com.zosh.client.UserOrgClient;
import com.zosh.domain.OrderStatus;
import com.zosh.domain.PaymentType;
import com.zosh.domain.PaymentSettlementStatus;
import com.zosh.modal.Customer;
import com.zosh.modal.Order;
import com.zosh.payload.dto.BranchSummaryDTO;
import com.zosh.payload.dto.CurrentUserDTO;
import com.zosh.payload.dto.OrderDTO;
import com.zosh.payload.dto.OrderItemDTO;
import com.zosh.payload.dto.ProductDTO;
import com.zosh.repository.CustomerRepository;
import com.zosh.repository.OrderRepository;
import com.zosh.service.OrderWorkflowCoordinator;
import com.zosh.service.OrderWorkflowProcessor;
import com.zosh.service.OrderPricingPolicy;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private InventoryClient inventoryClient;
    @Mock private FraudDetectionClient fraudDetectionClient;
    @Mock private UserOrgClient userOrgClient;
    @Mock private OrderWorkflowCoordinator orderWorkflowCoordinator;
    @Mock private OrderWorkflowProcessor orderWorkflowProcessor;
    private OrderServiceImpl service;

    @org.junit.jupiter.api.BeforeEach
    void createServiceWithFailClosedDiscountPolicy() {
        service = serviceWithPolicy(new OrderPricingPolicy(new BigDecimal("18")));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createOrderUsesCatalogPriceAndAuthenticatedBranchCashier() throws Exception {
        authenticateCashier(3L);
        prepareBranchAndUser();
        when(inventoryClient.getProductById(7L))
                .thenReturn(ProductDTO.builder()
                        .id(7L)
                        .storeId(8L)
                        .name("Catalog item")
                        .sku("CAT-7")
                        .sellingPrice(new BigDecimal("20.00"))
                        .build());
        AtomicReference<Order> pendingOrder = new AtomicReference<>();
        when(orderWorkflowCoordinator.startPending(any(Order.class), any(Map.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(55L);
            pendingOrder.set(saved);
            return 81L;
        });
        when(orderWorkflowProcessor.processOrder(81L)).thenAnswer(invocation -> {
            pendingOrder.get().setStatus(OrderStatus.COMPLETED);
            return pendingOrder.get();
        });

        OrderDTO request = OrderDTO.builder()
                .branchId(3L)
                .cashierId(999L)
                .paymentType(PaymentType.CASH)
                .paymentSettlementStatus(PaymentSettlementStatus.EXTERNAL_CONFIRMATION_REQUIRED)
                .status(OrderStatus.CANCELLED)
                .items(List.of(OrderItemDTO.builder()
                        .productId(7L)
                        .quantity(2)
                        .price(new BigDecimal("0.02"))
                        .product(ProductDTO.builder().sellingPrice(new BigDecimal("0.01")).build())
                        .build()))
                .build();

        OrderDTO result = service.createOrder(request);

        assertEquals(new BigDecimal("47.20"), result.getTotalAmount());
        assertEquals(new BigDecimal("40.00"), result.getSubtotalAmount());
        assertEquals(new BigDecimal("7.20"), result.getTaxAmount());
        assertEquals(42L, pendingOrder.get().getCashierId());
        assertEquals(3L, pendingOrder.get().getBranchId());
        assertEquals(OrderStatus.COMPLETED, result.getStatus());
        assertEquals(PaymentSettlementStatus.CASH_RECORDED, result.getPaymentSettlementStatus());
        assertEquals(new BigDecimal("20.00"), pendingOrder.get().getItems().get(0).getProductPrice());
        verify(orderWorkflowCoordinator).startPending(any(Order.class), any(Map.class));
        verify(inventoryClient, never()).decrementStock(any(), any(), any(), any());
    }

    @Test
    void cardPaymentCannotBeMarkedCashSettledByClientRequest() throws Exception {
        authenticateCashier(3L);
        prepareBranchAndUser();
        prepareProduct(7L, 8L, "20.00");
        AtomicReference<Order> pendingOrder = prepareSuccessfulWorkflow(85L, 58L);

        OrderDTO request = OrderDTO.builder()
                .branchId(3L)
                .paymentType(PaymentType.CARD)
                .paymentSettlementStatus(PaymentSettlementStatus.CASH_RECORDED)
                .items(List.of(OrderItemDTO.builder().productId(7L).quantity(1).build()))
                .build();

        OrderDTO result = service.createOrder(request);

        assertEquals(PaymentSettlementStatus.EXTERNAL_CONFIRMATION_REQUIRED,
                pendingOrder.get().getPaymentSettlementStatus());
        assertEquals(PaymentSettlementStatus.EXTERNAL_CONFIRMATION_REQUIRED,
                result.getPaymentSettlementStatus());
    }

    @Test
    void transientInventoryFailureLeavesTheOrderForDurableRecovery() throws Exception {
        authenticateCashier(3L);
        prepareBranchAndUser();
        when(inventoryClient.getProductById(7L))
                .thenReturn(product(7L, 8L, "20.00"));
        when(inventoryClient.getProductById(9L))
                .thenReturn(product(9L, 8L, "5.00"));
        when(orderWorkflowCoordinator.startPending(any(Order.class), any(Map.class))).thenReturn(82L);
        doThrow(new IllegalStateException("simulated timeout"))
                .when(orderWorkflowProcessor).processOrder(82L);

        OrderDTO request = OrderDTO.builder()
                .branchId(3L)
                .items(List.of(
                        OrderItemDTO.builder().productId(7L).quantity(2).build(),
                        OrderItemDTO.builder().productId(9L).quantity(1).build()))
                .build();

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.createOrder(request, "29da1c04-9100-4e6c-932e-d15c35248059"));
        verify(orderWorkflowCoordinator).startPending(any(Order.class), any(Map.class));
        verify(inventoryClient, never()).incrementStock(any(), any(), any(), any());
    }

    @Test
    void createOrderRejectsAnotherBranchBeforeReadingProducts() throws Exception {
        authenticateCashier(3L);
        when(userOrgClient.getBranch(3L)).thenReturn(new BranchSummaryDTO(3L, 8L));
        when(userOrgClient.canCurrentUserAccessBranch(3L, 8L, "ORDER_CREATE")).thenReturn(false);

        OrderDTO request = OrderDTO.builder()
                .branchId(3L)
                .items(List.of(OrderItemDTO.builder().productId(7L).quantity(1).build()))
                .build();

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.createOrder(request));
        verify(inventoryClient, never()).getProductById(eq(7L));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrderRejectsProductFromAnotherStore() throws Exception {
        authenticateCashier(3L);
        prepareBranchAndUser();
        when(inventoryClient.getProductById(7L))
                .thenReturn(product(7L, 9L, "20.00"));
        when(userOrgClient.canCurrentUserAccessBranch(3L, 9L, "ORDER_CREATE")).thenReturn(false);

        OrderDTO request = OrderDTO.builder()
                .branchId(3L)
                .items(List.of(OrderItemDTO.builder().productId(7L).quantity(1).build()))
                .build();

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.createOrder(request));
        verify(orderWorkflowCoordinator, never()).startPending(any(Order.class), any(Map.class));
    }

    @Test
    void createOrderRejectsNonPositiveQuantityBeforeReadingCatalog() throws Exception {
        authenticateCashier(3L);
        prepareBranchAndUser();

        OrderDTO request = OrderDTO.builder()
                .branchId(3L)
                .items(List.of(OrderItemDTO.builder().productId(7L).quantity(0).build()))
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.createOrder(request));
        verify(inventoryClient, never()).getProductById(eq(7L));
        verify(orderWorkflowCoordinator, never()).startPending(any(Order.class), any(Map.class));
    }

    @Test
    void smallCatalogAmountUsesExplicitHalfUpTaxRounding() throws Exception {
        authenticateCashier(3L);
        prepareBranchAndUser();
        when(inventoryClient.getProductById(7L))
                .thenReturn(product(7L, 8L, "0.05"));
        AtomicReference<Order> pendingOrder = new AtomicReference<>();
        when(orderWorkflowCoordinator.startPending(any(Order.class), any(Map.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(56L);
            pendingOrder.set(saved);
            return 83L;
        });
        when(orderWorkflowProcessor.processOrder(83L)).thenAnswer(invocation -> {
            pendingOrder.get().setStatus(OrderStatus.COMPLETED);
            return pendingOrder.get();
        });

        OrderDTO request = OrderDTO.builder()
                .branchId(3L)
                .items(List.of(OrderItemDTO.builder()
                        .productId(7L).quantity(3).price(new BigDecimal("0.01")).build()))
                .build();

        OrderDTO result = service.createOrder(request);

        assertEquals(new BigDecimal("0.15"), result.getSubtotalAmount());
        assertEquals(new BigDecimal("0.03"), result.getTaxAmount());
        assertEquals(new BigDecimal("0.18"), result.getTotalAmount());
        assertEquals(new BigDecimal("0.05"), pendingOrder.get().getItems().get(0).getProductPrice());
    }

    @Test
    void createOrderRejectsCatalogAmountOutsideDecimalColumnRange() throws Exception {
        authenticateCashier(3L);
        prepareBranchAndUser();
        when(inventoryClient.getProductById(7L))
                .thenReturn(product(7L, 8L, "100000000000000000.00"));

        OrderDTO request = OrderDTO.builder()
                .branchId(3L)
                .items(List.of(OrderItemDTO.builder().productId(7L).quantity(1).build()))
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.createOrder(request));
        verify(orderWorkflowCoordinator, never()).startPending(any(Order.class), any(Map.class));
    }

    @Test
    void rejectsClientSuppliedDiscountAmountBeforeStartingWorkflow() throws Exception {
        authenticateCashier(3L);
        prepareBranchAndUser();
        when(inventoryClient.getProductById(7L))
                .thenReturn(product(7L, 8L, "20.00"));

        OrderDTO request = OrderDTO.builder()
                .branchId(3L)
                .discountPercent(10.0)
                .discountAmount(new BigDecimal("1.00"))
                .items(List.of(OrderItemDTO.builder().productId(7L).quantity(2).build()))
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.createOrder(request));
        verify(orderWorkflowCoordinator, never()).startPending(any(Order.class), any(Map.class));
    }

    @Test
    void branchManagerCannotCreateOrders() throws Exception {
        authenticateBranchManager(3L);
        OrderDTO request = OrderDTO.builder()
                .branchId(3L)
                .items(List.of(OrderItemDTO.builder().productId(7L).quantity(1).build()))
                .build();

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.createOrder(request));
        verify(orderWorkflowCoordinator, never()).startPending(any(Order.class), any(Map.class));
    }

    @Test
    void negativeCashierDiscountIsRejectedBeforeWorkflowStarts() throws Exception {
        authenticateCashier(3L);
        prepareBranchAndUser();
        prepareProduct(7L, 8L, "20.00");
        OrderDTO request = OrderDTO.builder()
                .branchId(3L)
                .discountPercent(-0.01)
                .items(List.of(OrderItemDTO.builder().productId(7L).quantity(1).build()))
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> service.createOrder(request));
        verify(orderWorkflowCoordinator, never()).startPending(any(Order.class), any(Map.class));
    }

    @Test
    void cashierCannotSubmitAClientCalculatedAmountEvenWithApprovalFlag() throws Exception {
        authenticateCashier(3L);
        prepareBranchAndUser();
        prepareProduct(7L, 8L, "20.00");

        OrderDTO request = OrderDTO.builder()
                .branchId(3L)
                .discountAmount(new BigDecimal("1.00"))
                .items(List.of(OrderItemDTO.builder().productId(7L).quantity(1).build()))
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> service.createOrder(request));
        verify(orderWorkflowCoordinator, never()).startPending(any(Order.class), any(Map.class));
    }

    @Test
    void forgedApprovalFlagDoesNotAuthorizeClientCalculatedAmount() throws Exception {
        authenticateCashier(3L);
        prepareBranchAndUser();
        prepareProduct(7L, 8L, "20.00");
        OrderDTO request = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue("{\"branchId\":3,\"discountAmount\":1.00,\"approved\":true,"
                        + "\"items\":[{\"productId\":7,\"quantity\":1}]}", OrderDTO.class);

        assertThrows(IllegalArgumentException.class,
                () -> service.createOrder(request));
        verify(orderWorkflowCoordinator, never()).startPending(any(Order.class), any(Map.class));
    }

    @Test
    void cashierCanApplyBranchConfiguredPercentageAndServerCalculatesTaxAndTotal() throws Exception {
        authenticateCashier(3L);
        prepareBranchAndUser();
        when(userOrgClient.getBranch(3L)).thenReturn(
                new BranchSummaryDTO(3L, 8L, "Test branch", new BigDecimal("10.00")));
        prepareProduct(7L, 8L, "20.00");
        AtomicReference<Order> pendingOrder = prepareSuccessfulWorkflow(84L, 57L);
        OrderDTO request = OrderDTO.builder()
                .branchId(3L)
                .discountPercent(10.0)
                .items(List.of(OrderItemDTO.builder().productId(7L).quantity(1).build()))
                .build();

        OrderDTO result = service.createOrder(request);

        assertEquals(new BigDecimal("2.00"), result.getDiscountAmount());
        assertEquals(new BigDecimal("3.60"), result.getTaxAmount());
        assertEquals(new BigDecimal("21.60"), result.getTotalAmount());
        assertEquals(OrderStatus.COMPLETED, result.getStatus());
        assertEquals(42L, pendingOrder.get().getCashierId());
    }

    @Test
    void cashierCannotExceedTheBranchConfiguredPercentage() throws Exception {
        authenticateCashier(3L);
        prepareBranchAndUser();
        when(userOrgClient.getBranch(3L)).thenReturn(
                new BranchSummaryDTO(3L, 8L, "Test branch", new BigDecimal("10.00")));
        prepareProduct(7L, 8L, "20.00");

        OrderDTO request = OrderDTO.builder()
                .branchId(3L)
                .discountPercent(10.01)
                .items(List.of(OrderItemDTO.builder().productId(7L).quantity(1).build()))
                .build();

        assertThrows(IllegalArgumentException.class, () -> service.createOrder(request));
        verify(orderWorkflowCoordinator, never()).startPending(any(Order.class), any(Map.class));
    }

    @Test
    void invalidDiscountPrecisionIsRejectedBeforeWorkflowStarts() throws Exception {
        authenticateCashier(3L);
        prepareBranchAndUser();
        when(userOrgClient.getBranch(3L)).thenReturn(
                new BranchSummaryDTO(3L, 8L, "Test branch", new BigDecimal("15.00")));
        prepareProduct(7L, 8L, "20.00");
        OrderDTO request = OrderDTO.builder()
                .branchId(3L)
                .discountPercent(1.12345)
                .items(List.of(OrderItemDTO.builder().productId(7L).quantity(1).build()))
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> service.createOrder(request));
        verify(orderWorkflowCoordinator, never()).startPending(any(Order.class), any(Map.class));
    }

    @Test
    void repeatedOrderRequestReturnsSavedOrderWithoutDeductingStockAgain() throws Exception {
        authenticateCashier(3L);
        prepareBranchAndUser();
        String key = "9b18fc30-0abd-4b08-aad2-7b20c4723810";
        Order existing = Order.builder().id(55L).branchId(3L).cashierId(42L).build();
        when(orderRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existing));

        OrderDTO request = OrderDTO.builder()
                .branchId(3L)
                .items(List.of(OrderItemDTO.builder().productId(7L).quantity(1).build()))
                .build();

        OrderDTO result = service.createOrder(request, key);

        assertEquals(55L, result.getId());
        verify(inventoryClient, never()).decrementStock(any(), any(), any(), any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void idempotencyKeyCannotRevealAnOrderOwnedByAnotherCashier() throws Exception {
        authenticateCashier(3L);
        prepareBranchAndUser();
        String key = "9b18fc30-0abd-4b08-aad2-7b20c4723810";
        Order existing = Order.builder().id(55L).branchId(3L).cashierId(99L).build();
        when(orderRepository.findByIdempotencyKey(key)).thenReturn(Optional.of(existing));
        OrderDTO request = OrderDTO.builder()
                .branchId(3L)
                .items(List.of(OrderItemDTO.builder().productId(7L).quantity(1).build()))
                .build();

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.createOrder(request, key));
        verify(inventoryClient, never()).decrementStock(any(), any(), any(), any());
    }

    @Test
    void cashierPurchaseHistoryIncludesOnlyOrdersFromAccessibleBranches() {
        authenticateCashier(3L);
        Customer customer = Customer.builder().id(91L).fullName("Test Customer").storeId(8L).branchId(3L).build();
        when(customerRepository.findById(91L)).thenReturn(Optional.of(customer));
        when(orderRepository.findByCustomerId(91L)).thenReturn(List.of(
                Order.builder().id(101L).branchId(3L).customer(customer).build(),
                Order.builder().id(102L).branchId(4L).customer(customer).build()));
        when(userOrgClient.canCurrentUserAccessBranch(3L, 8L, "READ")).thenReturn(true);
        when(userOrgClient.canCurrentUserAccessBranch(3L, null, "READ")).thenReturn(true);
        when(userOrgClient.canCurrentUserAccessBranch(4L, null, "READ")).thenReturn(false);

        List<OrderDTO> result = service.getOrdersByCustomerId(91L);

        assertEquals(List.of(101L), result.stream().map(OrderDTO::getId).toList());
    }

    @Test
    void cashierCannotReadPurchaseHistoryForAnInaccessibleCustomer() {
        authenticateCashier(3L);
        Customer customer = Customer.builder().id(91L).fullName("Test Customer").storeId(8L).branchId(4L).build();
        when(customerRepository.findById(91L)).thenReturn(Optional.of(customer));
        when(orderRepository.findByCustomerId(91L)).thenReturn(List.of(
                Order.builder().id(102L).branchId(4L).customer(customer).build()));
        when(userOrgClient.canCurrentUserAccessBranch(4L, 8L, "READ")).thenReturn(false);
        when(userOrgClient.canCurrentUserAccessBranch(4L, null, "READ")).thenReturn(false);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.getOrdersByCustomerId(91L));
    }

    private void authenticateCashier(Long branchId) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "cashier@example.test",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_BRANCH_CASHIER")));
        authentication.setDetails(branchId);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void authenticateBranchManager(Long branchId) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "manager@example.test", null, List.of(new SimpleGrantedAuthority("ROLE_BRANCH_MANAGER")));
        authentication.setDetails(branchId);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void prepareBranchAndUser() {
        when(userOrgClient.getBranch(3L)).thenReturn(new BranchSummaryDTO(3L, 8L));
        when(userOrgClient.canCurrentUserAccessBranch(3L, 8L, "ORDER_CREATE")).thenReturn(true);
        when(userOrgClient.getCurrentUserProfile())
                .thenReturn(new CurrentUserDTO(42L, "cashier@example.test", "Cashier", 8L, 3L));
    }

    private ProductDTO product(Long id, Long storeId, String price) {
        return ProductDTO.builder()
                .id(id)
                .storeId(storeId)
                .name("Item " + id)
                .sku("SKU-" + id)
                .sellingPrice(new BigDecimal(price))
                .build();
    }

    private void prepareProduct(Long id, Long storeId, String price) {
        when(inventoryClient.getProductById(id))
                .thenReturn(product(id, storeId, price));
    }

    private AtomicReference<Order> prepareSuccessfulWorkflow(Long workflowId, Long orderId) {
        AtomicReference<Order> pendingOrder = new AtomicReference<>();
        when(orderWorkflowCoordinator.startPending(any(Order.class), any(Map.class))).thenAnswer(invocation -> {
            Order saved = invocation.getArgument(0);
            saved.setId(orderId);
            pendingOrder.set(saved);
            return workflowId;
        });
        when(orderWorkflowProcessor.processOrder(workflowId)).thenAnswer(invocation -> {
            pendingOrder.get().setStatus(OrderStatus.COMPLETED);
            return pendingOrder.get();
        });
        return pendingOrder;
    }

    private OrderServiceImpl serviceWithPolicy(OrderPricingPolicy policy) {
        return new OrderServiceImpl(orderRepository, customerRepository, inventoryClient,
                fraudDetectionClient, userOrgClient, orderWorkflowCoordinator,
                orderWorkflowProcessor, policy);
    }
}
