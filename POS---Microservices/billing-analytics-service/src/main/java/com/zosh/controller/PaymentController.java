package com.zosh.controller;

import com.razorpay.RazorpayException;
import com.stripe.exception.StripeException;
import com.zosh.client.UserOrgClient;
import com.zosh.domain.PaymentMethod;
import com.zosh.modal.PaymentOrder;
import com.zosh.payload.dto.CurrentUserDTO;
import com.zosh.payload.response.PaymentLinkResponse;
import com.zosh.payload.request.RazorpayPaymentVerificationRequest;
import com.zosh.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserOrgClient userOrgClient;

    @PostMapping("/create")
    @PreAuthorize("hasAnyAuthority('ROLE_STORE_ADMIN', 'ROLE_STORE_MANAGER')")
    public ResponseEntity<PaymentLinkResponse> createPaymentLink(
            @RequestParam Long storeId,
            @RequestParam Long planId,
            @RequestParam(defaultValue = "RAZORPAY") PaymentMethod paymentMethod,
            Authentication authentication) throws Exception, RazorpayException, StripeException {

        if (!Boolean.TRUE.equals(userOrgClient.canCurrentUserAccessStore(storeId))) {
            throw new AccessDeniedException("You are not authorized to manage payments for this store");
        }
        CurrentUserDTO currentUser = userOrgClient.getCurrentUserProfile();
        String email = currentUser.email();
        String fullName = currentUser.fullName();

        PaymentLinkResponse response = paymentService.createOrder(
                currentUser.id(), email, fullName, storeId, planId, paymentMethod);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/proceed")
    public ResponseEntity<Boolean> proceedPayment(
            @RequestBody RazorpayPaymentVerificationRequest request) throws Exception {

        Boolean success = paymentService.proceedPaymentOrder(
                request.paymentId(),
                request.paymentLinkId(),
                request.paymentLinkReferenceId(),
                request.paymentLinkStatus(),
                request.signature()
        );
        return ResponseEntity.ok(success);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentOrder> getPaymentOrderById(
            @PathVariable Long id,
            Authentication authentication) throws Exception {
        PaymentOrder paymentOrder = paymentService.getPaymentOrderById(id);
        boolean administrator = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!administrator && (authentication.getName() == null || paymentOrder.getUserEmail() == null
                || !authentication.getName().equalsIgnoreCase(paymentOrder.getUserEmail()))) {
            throw new AccessDeniedException("You are not authorized to view this payment order");
        }
        return ResponseEntity.ok(paymentOrder);
    }
}
