package com.zosh.controller;

import com.zosh.domain.OrderStatus;
import com.zosh.domain.PaymentType;
import com.zosh.exception.UserException;
import com.zosh.payload.dto.OrderDTO;
import com.zosh.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_BRANCH_CASHIER', 'ROLE_CASHIER')")
    public ResponseEntity<OrderDTO> createOrder(
            @RequestBody OrderDTO dto,
            @RequestHeader("Idempotency-Key") String idempotencyKey) throws UserException {
        if (idempotencyKey == null || !idempotencyKey.matches("[A-Fa-f0-9]{8}-(?:[A-Fa-f0-9]{4}-){3}[A-Fa-f0-9]{12}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A UUID Idempotency-Key header is required for order creation");
        }
        return ResponseEntity.ok(orderService.createOrder(dto, idempotencyKey));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER', 'ROLE_BRANCH_CASHIER', 'ROLE_CASHIER')")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER', 'ROLE_BRANCH_CASHIER', 'ROLE_CASHIER')")
    public ResponseEntity<List<OrderDTO>> getOrdersByBranch(
            @PathVariable Long branchId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long cashierId,
            @RequestParam(required = false) PaymentType paymentType,
            @RequestParam(required = false) OrderStatus status) {
        return ResponseEntity.ok(orderService.getOrdersByBranch(
                branchId,
                customerId,
                cashierId,
                paymentType,
                status
        ));
    }

    @GetMapping("/cashier/{cashierId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER', 'ROLE_BRANCH_CASHIER', 'ROLE_CASHIER')")
    public ResponseEntity<List<OrderDTO>> getOrdersByCashier(@PathVariable Long cashierId) {
        return ResponseEntity.ok(orderService.getOrdersByCashier(cashierId));
    }

    @GetMapping("/today/branch/{branchId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER', 'ROLE_BRANCH_CASHIER', 'ROLE_CASHIER')")
    public ResponseEntity<List<OrderDTO>> getTodayOrders(@PathVariable Long branchId) {
        return ResponseEntity.ok(orderService.getTodayOrdersByBranch(branchId));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER', 'ROLE_BRANCH_CASHIER', 'ROLE_CASHIER')")
    public ResponseEntity<List<OrderDTO>> getCustomerOrders(@PathVariable Long customerId) {
        return ResponseEntity.ok(orderService.getOrdersByCustomerId(customerId));
    }

    @GetMapping("/recent/{branchId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER', 'ROLE_BRANCH_CASHIER', 'ROLE_CASHIER')")
    public ResponseEntity<List<OrderDTO>> getRecentOrders(@PathVariable Long branchId) {
        List<OrderDTO> recentOrders = orderService.getTop5RecentOrdersByBranchId(branchId);
        return ResponseEntity.ok(recentOrders);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER')")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER', 'ROLE_BRANCH_MANAGER')")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id));
    }
}
