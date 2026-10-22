package com.zosh.controller;

import com.razorpay.RazorpayException;
import com.stripe.exception.StripeException;
import com.zosh.domain.PaymentMethod;
import com.zosh.modal.PaymentOrder;
import com.zosh.payload.response.PaymentLinkResponse;
import com.zosh.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<PaymentLinkResponse> createPaymentLink(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "X-User-FullName", required = false) String userFullName,
            @RequestHeader(value = "X-Store-Id", required = false) Long storeId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam Long planId,
            @RequestParam(defaultValue = "RAZORPAY") PaymentMethod paymentMethod,
            Authentication authentication) throws Exception, RazorpayException, StripeException {

        String email = userEmail != null ? userEmail : authentication.getName();
        String fullName = userFullName != null ? userFullName : email;

        PaymentLinkResponse response = paymentService.createOrder(
                userId, email, fullName, storeId, planId, paymentMethod);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/proceed")
    public ResponseEntity<Boolean> proceedPayment(
            @RequestParam String paymentId,
            @RequestParam String paymentLinkId) throws Exception {

        PaymentOrder paymentOrder = paymentService.getPaymentOrderByPaymentId(paymentLinkId);
        Boolean success = paymentService.proceedPaymentOrder(paymentOrder, paymentId, paymentLinkId);
        return ResponseEntity.ok(success);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentOrder> getPaymentOrderById(@PathVariable Long id) throws Exception {
        return ResponseEntity.ok(paymentService.getPaymentOrderById(id));
    }
}
