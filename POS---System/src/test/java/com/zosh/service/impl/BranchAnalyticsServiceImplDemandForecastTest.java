package com.zosh.service.impl;

import com.zosh.payload.dto.ProductDemandForecastDTO;
import com.zosh.repository.InventoryRepository;
import com.zosh.repository.OrderItemRepository;
import com.zosh.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchAnalyticsServiceImplDemandForecastTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private BranchAnalyticsServiceImpl branchAnalyticsService;

    @Test
    void demandForecastCalculatesReorderQtyFromSeasonalProjection() {
        LocalDate anchorDate = LocalDate.of(2026, 4, 24);
        List<Object[]> demandRows = new ArrayList<>();
        LocalDate historyStart = anchorDate.minusDays(14);
        for (int i = 0; i < 14; i++) {
            demandRows.add(new Object[]{
                    101L,
                    "Test Product",
                    Date.valueOf(historyStart.plusDays(i)),
                    10L
            });
        }

        when(orderItemRepository.getDailyProductDemandBetween(eq(7L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(demandRows);
        when(inventoryRepository.getProductStockByBranch(7L))
                .thenReturn(List.<Object[]>of(new Object[]{101L, "Test Product", 50L}));

        List<ProductDemandForecastDTO> result = branchAnalyticsService.getDemandForecast(7L, List.of(7, 14, 30), 14, anchorDate);

        assertEquals(1, result.size());
        ProductDemandForecastDTO forecast = result.get(0);
        assertEquals(101L, forecast.getProductId());
        assertEquals(70.0, forecast.getForecast7(), 0.01);
        assertEquals(140.0, forecast.getForecast14(), 0.01);
        assertEquals(300.0, forecast.getForecast30(), 0.01);
        assertEquals(250, forecast.getRecommendedReorderQty());
        assertEquals(30, forecast.getRecommendedHorizonDays());
        assertTrue(forecast.isReorderSuggested());
    }

    @Test
    void demandForecastReturnsZeroReorderWhenNoDemandHistoryExists() {
        LocalDate anchorDate = LocalDate.of(2026, 4, 24);
        when(orderItemRepository.getDailyProductDemandBetween(eq(11L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(inventoryRepository.getProductStockByBranch(11L))
                .thenReturn(List.<Object[]>of(new Object[]{333L, "Slow Product", 12L}));

        List<ProductDemandForecastDTO> result = branchAnalyticsService.getDemandForecast(11L, List.of(7, 14, 30), 30, anchorDate);

        assertEquals(1, result.size());
        ProductDemandForecastDTO forecast = result.get(0);
        assertEquals(0.0, forecast.getForecast7(), 0.01);
        assertEquals(0.0, forecast.getForecast14(), 0.01);
        assertEquals(0.0, forecast.getForecast30(), 0.01);
        assertEquals(0, forecast.getRecommendedReorderQty());
        assertFalse(forecast.isReorderSuggested());
    }
}
