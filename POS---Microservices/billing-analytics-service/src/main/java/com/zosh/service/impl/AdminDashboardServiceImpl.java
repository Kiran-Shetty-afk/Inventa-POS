package com.zosh.service.impl;

import com.zosh.payload.AdminAnalysis.DashboardSummaryDTO;
import com.zosh.payload.AdminAnalysis.StoreDashboardStatsDTO;
import com.zosh.payload.AdminAnalysis.StoreRegistrationStatDTO;
import com.zosh.payload.AdminAnalysis.StoreStatusDistributionDTO;
import com.zosh.repository.SubscriptionRepository;
import com.zosh.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final SubscriptionRepository subscriptionRepository;
    private final RestTemplate restTemplate;

    @Value("${services.user-org.url:http://localhost:8081}")
    private String userOrgServiceUrl;

    @Override
    public DashboardSummaryDTO getDashboardSummary(String authorization) {
        Double totalRevenue = subscriptionRepository.findAll().stream()
                .filter(s -> s.getPaymentStatus() != null &&
                        s.getPaymentStatus().name().equals("PAID"))
                .mapToDouble(s -> s.getPlan() != null ? s.getPlan().getPrice() : 0.0)
                .sum();

        // Try to get total/active/pending stores from user-org-service
        Long totalStores = 0L, activeStores = 0L, pendingStores = 0L, blockedStores = 0L;
        try {
            ResponseEntity<StoreDashboardStatsDTO> storeStats = restTemplate.exchange(
                    userOrgServiceUrl + "/api/stores/stats", HttpMethod.GET,
                    authorizedRequest(authorization), StoreDashboardStatsDTO.class);
            StoreDashboardStatsDTO stats = storeStats.getBody();
            if (stats != null) {
                totalStores = zeroIfNull(stats.getTotal());
                activeStores = zeroIfNull(stats.getActive());
                pendingStores = zeroIfNull(stats.getPending());
                blockedStores = zeroIfNull(stats.getBlocked());
            }
        } catch (Exception e) {
            log.warn("Could not fetch store stats from user-org-service: {}", e.getMessage());
        }

        return DashboardSummaryDTO.builder()
                .totalStores(totalStores)
                .activeStores(activeStores)
                .pendingStores(pendingStores)
                .blockedStores(blockedStores)
                .totalPlatformRevenue(totalRevenue)
                .build();
    }

    @Override
    public List<StoreRegistrationStatDTO> getLast7DayRegistrationStats(String authorization) {
        try {
            ResponseEntity<List<StoreRegistrationStatDTO>> response = restTemplate.exchange(
                    userOrgServiceUrl + "/api/stores/registration-stats",
                    HttpMethod.GET, authorizedRequest(authorization),
                    new ParameterizedTypeReference<List<StoreRegistrationStatDTO>>() {});
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Could not fetch registration stats: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public StoreStatusDistributionDTO getStoreStatusDistribution(String authorization) {
        try {
            ResponseEntity<StoreDashboardStatsDTO> response = restTemplate.exchange(
                    userOrgServiceUrl + "/api/stores/stats", HttpMethod.GET,
                    authorizedRequest(authorization), StoreDashboardStatsDTO.class);
            StoreDashboardStatsDTO stats = response.getBody();
            if (stats != null) {
                return StoreStatusDistributionDTO.builder()
                        .total(zeroIfNull(stats.getTotal()))
                        .active(zeroIfNull(stats.getActive()))
                        .pending(zeroIfNull(stats.getPending()))
                        .blocked(zeroIfNull(stats.getBlocked()))
                        .closed(zeroIfNull(stats.getClosed()))
                        .build();
            }
        } catch (Exception e) {
            log.warn("Could not fetch store status distribution: {}", e.getMessage());
        }
        return StoreStatusDistributionDTO.builder()
                .total(0L).active(0L).pending(0L).blocked(0L).closed(0L)
                .build();
    }

    private HttpEntity<Void> authorizedRequest(String authorization) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        return new HttpEntity<>(headers);
    }

    private long zeroIfNull(Long value) {
        return value == null ? 0L : value;
    }
}
