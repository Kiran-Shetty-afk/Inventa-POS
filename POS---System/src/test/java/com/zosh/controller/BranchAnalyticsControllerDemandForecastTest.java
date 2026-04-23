package com.zosh.controller;

import com.zosh.payload.dto.ProductDemandForecastDTO;
import com.zosh.payload.dto.BranchHealthCopilotRequestDTO;
import com.zosh.payload.dto.BranchHealthCopilotResponseDTO;
import com.zosh.service.BranchAnalyticsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchAnalyticsControllerDemandForecastTest {

    @Mock
    private BranchAnalyticsService branchAnalyticsService;

    @InjectMocks
    private BranchAnalyticsController branchAnalyticsController;

    @Test
    void getDemandForecastDelegatesToServiceAndReturnsPayload() {
        List<ProductDemandForecastDTO> expected = List.of(
                ProductDemandForecastDTO.builder()
                        .productId(1L)
                        .productName("Milk")
                        .forecast7(42.0)
                        .recommendedReorderQty(10)
                        .build()
        );

        when(branchAnalyticsService.getDemandForecast(8L, List.of(7, 14, 30), 90, LocalDate.of(2026, 4, 23)))
                .thenReturn(expected);

        ResponseEntity<List<ProductDemandForecastDTO>> response = branchAnalyticsController.getDemandForecast(
                8L,
                List.of(7, 14, 30),
                90,
                LocalDate.of(2026, 4, 23)
        );

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(branchAnalyticsService).getDemandForecast(8L, List.of(7, 14, 30), 90, LocalDate.of(2026, 4, 23));
    }

    @Test
    void generateHealthCopilotSummaryDelegatesToServiceAndReturnsPayload() {
        BranchHealthCopilotRequestDTO request = new BranchHealthCopilotRequestDTO();
        request.setBranchId(8L);
        request.setDate(LocalDate.of(2026, 4, 23));

        BranchHealthCopilotResponseDTO expected = BranchHealthCopilotResponseDTO.builder()
                .headline("Health summary")
                .summary("Summary body")
                .build();

        when(branchAnalyticsService.generateHealthCopilotSummary(request)).thenReturn(expected);

        ResponseEntity<BranchHealthCopilotResponseDTO> response =
                branchAnalyticsController.generateHealthCopilotSummary(request);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(expected, response.getBody());
        verify(branchAnalyticsService).generateHealthCopilotSummary(request);
    }
}
