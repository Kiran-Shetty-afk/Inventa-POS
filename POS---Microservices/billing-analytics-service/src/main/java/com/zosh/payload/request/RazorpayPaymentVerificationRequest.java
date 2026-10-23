package com.zosh.payload.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RazorpayPaymentVerificationRequest(
        @JsonProperty("razorpay_payment_id") String paymentId,
        @JsonProperty("razorpay_payment_link_id") String paymentLinkId,
        @JsonProperty("razorpay_payment_link_reference_id") String paymentLinkReferenceId,
        @JsonProperty("razorpay_payment_link_status") String paymentLinkStatus,
        @JsonProperty("razorpay_signature") String signature
) {
}
