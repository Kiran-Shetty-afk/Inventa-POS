package com.zosh.service.impl;

import com.razorpay.Payment;
import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.zosh.domain.PaymentMethod;
import com.zosh.domain.PaymentOrderStatus;
import com.zosh.modal.PaymentOrder;
import com.zosh.modal.SubscriptionPlan;
import com.zosh.payload.response.PaymentLinkResponse;
import com.zosh.repository.PaymentOrderRepository;
import com.zosh.repository.SubscriptionPlanRepository;
import com.zosh.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    @Value("${stripe.api.key:sk_test_placeholder}")
    private String stripeSecretKey;

    @Value("${razorpay.api.key:rzp_test_placeholder}")
    private String razorpayApiKey;

    @Value("${razorpay.api.secret:placeholder_secret}")
    private String razorpayApiSecret;

    private final PaymentOrderRepository paymentOrderRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    @Override
    public PaymentLinkResponse createOrder(Long userId,
                                           String userEmail,
                                           String userFullName,
                                           Long storeId,
                                           Long planId,
                                           PaymentMethod paymentMethod) throws RazorpayException, StripeException, Exception {

        SubscriptionPlan subscriptionPlan = subscriptionPlanRepository.findById(planId)
                .orElseThrow(() -> new Exception("Subscription plan not found with id: " + planId));

        Double amount = subscriptionPlan.getPrice();

        PaymentOrder order = PaymentOrder.builder()
                .userId(userId)
                .userEmail(userEmail)
                .storeId(storeId)
                .amount(amount)
                .planId(planId)
                .paymentMethod(paymentMethod)
                .build();

        PaymentOrder paymentOrder = paymentOrderRepository.save(order);

        PaymentLinkResponse res = new PaymentLinkResponse();

        if (paymentMethod.equals(PaymentMethod.RAZORPAY)) {
            PaymentLink payment = createRazorpayPaymentLink(userEmail, userFullName,
                    paymentOrder.getAmount(), paymentOrder.getId());
            String paymentUrl = payment.get("short_url");
            String paymentUrlId = payment.get("id");
            res.setPayment_link_url(paymentUrl);
            res.setPayment_link_id(paymentUrlId);
            paymentOrder.setPaymentLinkId(paymentUrlId);
        } else {
            String paymentLink = createStripePaymentLink(paymentOrder.getAmount(), paymentOrder.getPlanId());
            res.setPayment_link_url(paymentLink);
        }

        paymentOrderRepository.save(paymentOrder);
        return res;
    }

    @Override
    public PaymentOrder getPaymentOrderById(Long id) throws Exception {
        return paymentOrderRepository.findById(id)
                .orElseThrow(() -> new Exception("Payment order not found with id: " + id));
    }

    @Override
    public PaymentOrder getPaymentOrderByPaymentId(String paymentLinkId) throws Exception {
        return paymentOrderRepository.findByPaymentLinkId(paymentLinkId)
                .orElseThrow(() -> new Exception("Payment order not found with paymentLinkId: " + paymentLinkId));
    }

    @Override
    public Boolean proceedPaymentOrder(PaymentOrder paymentOrder,
                                       String paymentId,
                                       String paymentLinkId) throws RazorpayException {
        if (paymentOrder.getStatus().equals(PaymentOrderStatus.PENDING)) {
            RazorpayClient razorpay = new RazorpayClient(razorpayApiKey, razorpayApiSecret);
            Payment payment = razorpay.payments.fetch(paymentId);
            String status = payment.get("status");

            if ("captured".equals(status)) {
                paymentOrder.setStatus(PaymentOrderStatus.SUCCESS);
                paymentOrderRepository.save(paymentOrder);
                return true;
            }

            paymentOrder.setStatus(PaymentOrderStatus.FAILED);
            paymentOrderRepository.save(paymentOrder);
            return false;
        }
        return false;
    }

    @Override
    public PaymentLink createRazorpayPaymentLink(String userEmail,
                                                  String userFullName,
                                                  Double amount,
                                                  Long orderId) throws RazorpayException {
        double paise = amount * 100;
        try {
            RazorpayClient razorpay = new RazorpayClient(razorpayApiKey, razorpayApiSecret);

            JSONObject paymentLinkRequest = new JSONObject();
            paymentLinkRequest.put("amount", paise);
            paymentLinkRequest.put("currency", "INR");

            JSONObject customer = new JSONObject();
            customer.put("name", userFullName);
            customer.put("email", userEmail);
            paymentLinkRequest.put("customer", customer);

            JSONObject notify = new JSONObject();
            notify.put("email", true);
            paymentLinkRequest.put("notify", notify);

            paymentLinkRequest.put("reminder_enable", true);
            paymentLinkRequest.put("callback_url", "http://localhost:5173/payment-success/" + orderId);
            paymentLinkRequest.put("callback_method", "get");

            return razorpay.paymentLink.create(paymentLinkRequest);
        } catch (RazorpayException e) {
            throw new RazorpayException(e.getMessage());
        }
    }

    @Override
    public String createStripePaymentLink(Double amount, Long planId) throws StripeException {
        Stripe.apiKey = stripeSecretKey;

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("http://localhost:5173/payment-success/" + planId)
                .setCancelUrl("http://localhost:5173/payment/cancel")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("usd")
                                .setUnitAmount((long) (amount * 100))
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Inventa POS Subscription")
                                        .build())
                                .build())
                        .build())
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }
}
