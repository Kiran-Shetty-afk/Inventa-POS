package com.zosh.service.impl;

import com.razorpay.Payment;
import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
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
import com.zosh.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    @Value("${stripe.api.key:}")
    private String stripeSecretKey;

    @Value("${razorpay.api.key:rzp_test_placeholder}")
    private String razorpayApiKey;

    @Value("${razorpay.api.secret:placeholder_secret}")
    private String razorpayApiSecret;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    private final PaymentOrderRepository paymentOrderRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionService subscriptionService;

    @Override
    public PaymentLinkResponse createOrder(Long userId,
                                           String userEmail,
                                           String userFullName,
                                           Long storeId,
                                           Long planId,
                                           PaymentMethod paymentMethod) throws RazorpayException, StripeException, Exception {

        if (storeId == null) {
            throw new IllegalArgumentException("A store is required for a subscription payment.");
        }

        if (paymentMethod == PaymentMethod.STRIPE && !isStripeConfigured()) {
            throw new IllegalStateException("Stripe payments are disabled because STRIPE_API_KEY is not configured");
        }

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
    @Transactional
    public Boolean proceedPaymentOrder(String paymentId,
                                       String paymentLinkId,
                                       String paymentLinkReferenceId,
                                       String paymentLinkStatus,
                                       String signature) throws RazorpayException {
        if (paymentLinkId == null || paymentLinkId.isBlank()) {
            return false;
        }
        PaymentOrder paymentOrder = paymentOrderRepository.findByPaymentLinkIdForUpdate(paymentLinkId)
                .orElse(null);
        if (paymentOrder == null
                || paymentOrder.getPaymentMethod() != PaymentMethod.RAZORPAY
                || !String.valueOf(paymentOrder.getId()).equals(paymentLinkReferenceId)
                || !"paid".equalsIgnoreCase(paymentLinkStatus)
                || paymentId == null
                || paymentId.isBlank()
                || signature == null
                || signature.isBlank()) {
            return false;
        }

        JSONObject signatureAttributes = new JSONObject()
                .put("payment_link_id", paymentLinkId)
                .put("payment_link_reference_id", paymentLinkReferenceId)
                .put("payment_link_status", paymentLinkStatus)
                .put("razorpay_payment_id", paymentId)
                .put("razorpay_signature", signature);
        if (!Utils.verifyPaymentLink(signatureAttributes, razorpayApiSecret)) {
            return false;
        }

        if (paymentOrder.getStatus() == PaymentOrderStatus.SUCCESS) {
            return true;
        }
        if (paymentOrder.getStatus() != PaymentOrderStatus.PENDING
                || paymentOrder.getStoreId() == null
                || paymentOrder.getAmount() == null) {
            return false;
        }

        RazorpayClient razorpay = new RazorpayClient(razorpayApiKey, razorpayApiSecret);
        Payment payment = razorpay.payments.fetch(paymentId);
        Object amountValue = payment.get("amount");
        String status = payment.get("status");
        String currency = payment.get("currency");
        long expectedAmount = BigDecimal.valueOf(paymentOrder.getAmount()).movePointRight(2).longValueExact();

        if (!(amountValue instanceof Number amount)
                || amount.longValue() != expectedAmount
                || !"INR".equalsIgnoreCase(currency)
                || !"captured".equalsIgnoreCase(status)) {
            return false;
        }

        paymentOrder.setStatus(PaymentOrderStatus.SUCCESS);
        paymentOrderRepository.save(paymentOrder);
        subscriptionService.completePaidUpgrade(
                paymentOrder.getStoreId(),
                paymentOrder.getPlanId(),
                PaymentMethod.RAZORPAY.name(),
                paymentId
        );
        return true;
    }

    @Override
    public PaymentLink createRazorpayPaymentLink(String userEmail,
                                                  String userFullName,
                                                  Double amount,
                                                  Long orderId) throws RazorpayException {
        long paise = BigDecimal.valueOf(amount).movePointRight(2).longValueExact();
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
            paymentLinkRequest.put("reference_id", String.valueOf(orderId));
            paymentLinkRequest.put("callback_url", frontendUrl("/store/payment-success/" + orderId));
            paymentLinkRequest.put("callback_method", "get");

            return razorpay.paymentLink.create(paymentLinkRequest);
        } catch (RazorpayException e) {
            throw new RazorpayException(e.getMessage());
        }
    }

    @Override
    public String createStripePaymentLink(Double amount, Long planId) throws StripeException {
        if (!isStripeConfigured()) {
            throw new IllegalStateException("Stripe payments are disabled because STRIPE_API_KEY is not configured");
        }
        Stripe.apiKey = stripeSecretKey;

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(frontendUrl("/payment-success/" + planId))
                .setCancelUrl(frontendUrl("/payment/cancel"))
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

    private boolean isStripeConfigured() {
        return stripeSecretKey != null && !stripeSecretKey.isBlank();
    }

    private String frontendUrl(String path) {
        return frontendBaseUrl.replaceAll("/+$", "") + path;
    }
}
