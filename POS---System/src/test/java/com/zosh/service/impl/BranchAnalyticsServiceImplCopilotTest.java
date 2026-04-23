package com.zosh.service.impl;

import com.zosh.payload.dto.BranchHealthCopilotRequestDTO;
import com.zosh.payload.dto.BranchHealthCopilotResponseDTO;
import com.zosh.repository.InventoryRepository;
import com.zosh.repository.OrderItemRepository;
import com.zosh.repository.OrderRepository;
import com.zosh.repository.RefundRepository;
import com.zosh.service.BranchHealthNarrativeGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchAnalyticsServiceImplCopilotTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private BranchHealthNarrativeGenerator branchHealthNarrativeGenerator;

    @InjectMocks
    private BranchAnalyticsServiceImpl branchAnalyticsService;

    @Test
    void copilotSummaryFallsBackWhenNarrativeGeneratorFails() {
        BranchHealthCopilotRequestDTO request = new BranchHealthCopilotRequestDTO();
        request.setBranchId(2L);
        request.setDate(LocalDate.of(2026, 4, 23));

        when(orderRepository.getTotalSalesBetween(eq(2L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(BigDecimal.valueOf(10000.0)), Optional.of(BigDecimal.valueOf(8000.0)));
        when(orderRepository.findByBranchIdAndCreatedAtBetween(eq(2L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(orderRepository.getTopCashiersByRevenueBetween(eq(2L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.<Object[]>of(new Object[]{99L, "Cashier A", 3000.0}));
        when(orderItemRepository.getCategoryWiseSales(eq(2L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.<Object[]>of(new Object[]{"Beverages", 4500.0, 120L}));
        when(inventoryRepository.countLowStockItems(2L)).thenReturn(3);
        when(refundRepository.getBranchRefundSummary(eq(2L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new Object[]{4L, 800.0});
        when(refundRepository.getBranchRefundHourlySummary(eq(2L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.<Object[]>of(new Object[]{18, 3L, 600.0}));
        when(branchHealthNarrativeGenerator.generateNarrative(any()))
                .thenThrow(new IllegalStateException("LLM unavailable"));

        BranchHealthCopilotResponseDTO response = branchAnalyticsService.generateHealthCopilotSummary(request);

        assertNotNull(response);
        assertNotNull(response.getSummary());
        assertEquals("Branch health summary", response.getHeadline());
        assertNotNull(response.getSupportingMetrics());
        assertEquals(18, response.getSupportingMetrics().getRefundSpikeHour());
    }

    @Test
    void copilotSummaryUsesNarrativeGeneratorOutputWhenAvailable() {
        BranchHealthCopilotRequestDTO request = new BranchHealthCopilotRequestDTO();
        request.setBranchId(3L);
        request.setYear(2026);
        request.setMonth(4);

        when(orderRepository.getTotalSalesBetween(eq(3L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Optional.of(BigDecimal.valueOf(12000.0)), Optional.of(BigDecimal.valueOf(9000.0)));
        when(orderRepository.findByBranchIdAndCreatedAtBetween(eq(3L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(orderRepository.getTopCashiersByRevenueBetween(eq(3L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.<Object[]>of(new Object[]{101L, "Cashier B", 5000.0}));
        when(orderItemRepository.getCategoryWiseSales(eq(3L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.<Object[]>of(new Object[]{"Snacks", 3800.0, 90L}));
        when(inventoryRepository.countLowStockItems(3L)).thenReturn(2);
        when(refundRepository.getBranchRefundSummary(eq(3L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new Object[]{1L, 120.0});
        when(refundRepository.getBranchRefundHourlySummary(eq(3L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(branchHealthNarrativeGenerator.generateNarrative(any()))
                .thenReturn(BranchHealthCopilotResponseDTO.builder()
                        .headline("AI Headline")
                        .summary("AI Summary")
                        .highlights(List.of("h1"))
                        .risks(List.of("r1"))
                        .recommendedActions(List.of("a1"))
                        .build());

        BranchHealthCopilotResponseDTO response = branchAnalyticsService.generateHealthCopilotSummary(request);

        assertEquals("AI Headline", response.getHeadline());
        assertEquals("AI Summary", response.getSummary());
        assertTrue(response.getGeneratedAt() != null);
        assertEquals("Snacks", response.getSupportingMetrics().getTopCategoryName());
    }
}
