package com.zosh.controller;

import com.zosh.client.UserOrgClient;
import com.zosh.modal.PaymentOrder;
import com.zosh.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentOwnershipTest {

    @Test
    void paymentOrderDetailsAreDeniedToAnotherStoreUser() throws Exception {
        PaymentService payments = mock(PaymentService.class);
        PaymentOrder order = PaymentOrder.builder()
                .id(5L)
                .userEmail("owner@example.invalid")
                .build();
        when(payments.getPaymentOrderById(5L)).thenReturn(order);
        PaymentController controller = new PaymentController(payments, mock(UserOrgClient.class));
        var requester = new UsernamePasswordAuthenticationToken(
                "other@example.invalid", null, List.of(new SimpleGrantedAuthority("ROLE_STORE_ADMIN")));

        assertThrows(AccessDeniedException.class, () -> controller.getPaymentOrderById(5L, requester));
    }

    @Test
    void paymentOrderDetailsRemainAvailableToTheirOwner() throws Exception {
        PaymentService payments = mock(PaymentService.class);
        PaymentOrder order = PaymentOrder.builder()
                .id(5L)
                .userEmail("owner@example.invalid")
                .build();
        when(payments.getPaymentOrderById(5L)).thenReturn(order);
        PaymentController controller = new PaymentController(payments, mock(UserOrgClient.class));
        var requester = new UsernamePasswordAuthenticationToken(
                "owner@example.invalid", null, List.of(new SimpleGrantedAuthority("ROLE_STORE_ADMIN")));

        assertEquals(order, controller.getPaymentOrderById(5L, requester).getBody());
    }
}
