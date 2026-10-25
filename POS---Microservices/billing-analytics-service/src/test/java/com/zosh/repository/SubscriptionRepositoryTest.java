package com.zosh.repository;

import com.zosh.domain.BillingCycle;
import com.zosh.domain.PaymentStatus;
import com.zosh.domain.SubscriptionStatus;
import com.zosh.modal.Subscription;
import com.zosh.modal.SubscriptionPlan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:billing-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class SubscriptionRepositoryTest {

    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private SubscriptionPlanRepository planRepository;

    @Test
    void revenueAggregationCountsOnlyPaidSubscriptions() {
        SubscriptionPlan plan = savePlan("Paid", new BigDecimal("12.50"));
        saveSubscription(plan, PaymentStatus.PAID, SubscriptionStatus.ACTIVE, LocalDate.now().plusDays(5));
        saveSubscription(plan, PaymentStatus.PENDING, SubscriptionStatus.TRIAL, LocalDate.now().plusDays(5));

        assertEquals(new BigDecimal("12.50"),
                subscriptionRepository.sumPlanPricesByPaymentStatus(PaymentStatus.PAID));
    }

    @Test
    void expirationUsesBulkDateRangeUpdateAndPreservesCurrentSubscriptions() {
        SubscriptionPlan plan = savePlan("Expiry", new BigDecimal("9.99"));
        Subscription expired = saveSubscription(
                plan, PaymentStatus.PAID, SubscriptionStatus.ACTIVE, LocalDate.now().minusDays(1));
        Subscription active = saveSubscription(
                plan, PaymentStatus.PAID, SubscriptionStatus.ACTIVE, LocalDate.now().plusDays(1));

        int updated = subscriptionRepository.expireEndedSubscriptions(
                LocalDate.now(), LocalDateTime.now(), SubscriptionStatus.EXPIRED);

        assertEquals(1, updated);
        assertEquals(SubscriptionStatus.EXPIRED,
                subscriptionRepository.findById(expired.getId()).orElseThrow().getStatus());
        assertEquals(SubscriptionStatus.ACTIVE,
                subscriptionRepository.findById(active.getId()).orElseThrow().getStatus());
    }

    private SubscriptionPlan savePlan(String name, BigDecimal price) {
        return planRepository.saveAndFlush(SubscriptionPlan.builder()
                .name(name)
                .description("Isolated repository test plan")
                .price(price)
                .billingCycle(BillingCycle.MONTHLY)
                .maxBranches(1)
                .maxUsers(1)
                .maxProducts(1)
                .active(true)
                .build());
    }

    private Subscription saveSubscription(SubscriptionPlan plan, PaymentStatus paymentStatus,
                                          SubscriptionStatus status, LocalDate endDate) {
        return subscriptionRepository.saveAndFlush(Subscription.builder()
                .storeId((long) (100 + subscriptionRepository.count()))
                .storeName("Isolated test store")
                .plan(plan)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(endDate)
                .status(status)
                .paymentStatus(paymentStatus)
                .build());
    }
}
