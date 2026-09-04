package com.zosh.service;

import com.razorpay.PaymentLink;
import com.razorpay.RazorpayException;
import com.stripe.exception.StripeException;
import com.zosh.domain.PaymentMethod;
import com.zosh.modal.PaymentOrder;
import com.zosh.payload.response.PaymentLinkResponse;

public interface PaymentService {

    PaymentLinkResponse createOrder(Long userId,
                                    String userEmail,
                                    String userFullName,
                                    Long storeId,
                                    Long planId,
                                    PaymentMethod paymentMethod) throws RazorpayException, StripeException, Exception;

    PaymentOrder getPaymentOrderById(Long id) throws Exception;

    PaymentOrder getPaymentOrderByPaymentId(String paymentLinkId) throws Exception;

    Boolean proceedPaymentOrder(PaymentOrder paymentOrder,
                                String paymentId,
                                String paymentLinkId) throws RazorpayException;

    PaymentLink createRazorpayPaymentLink(String userEmail,
                                          String userFullName,
                                          Double amount,
                                          Long orderId) throws RazorpayException;

    String createStripePaymentLink(Double amount, Long planId) throws StripeException;
}
