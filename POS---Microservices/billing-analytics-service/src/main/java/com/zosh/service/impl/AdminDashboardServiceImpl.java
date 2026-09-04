package com.zosh.service.impl;

import com.zosh.domain.SubscriptionStatus;
import com.zosh.payload.AdminAnalysis.DashboardSummaryDTO;
import com.zosh.payload.AdminAnalysis.StoreRegistrationStatDTO;
import com.zosh.payload.AdminAnalysis.StoreStatusDistributionDTO;
import com.zosh.repository.SubscriptionRepository;
import com.zosh.service.AdminDashboardService;
import com.zosh.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final RestTemplate restTemplate;

    @Value("${services.user-org.url:http://localhost:8081}")
    private String userOrgServiceUrl;

    @Override
    public DashboardSummaryDTO getDashboardSummary() {
        Long activeSubscriptions = subscriptionService.countByStatus(SubscriptionStatus.ACTIVE);
        Double totalRevenue = subscriptionRepository.findAll().stream()
                .filter(s -> s.getPaymentStatus() != null &&
                        s.getPaymentStatus().name().equals("PAID"))
                .mapToDouble(s -> s.getPlan() != null ? s.getPlan().getPrice() : 0.0)
                .sum();

        // Try to get total/active/pending stores from user-org-service
        Long totalStores = 0L, activeStores = activeSubscriptions, pendingStores = 0L;
        try {
            ResponseEntity<Map> storeStats = restTemplate.exchange(
                    userOrgServiceUrl + "/api/stores/stats", HttpMethod.GET, null, Map.class);
            if (storeStats.getBody() != null) {
                totalStores = Long.parseLong(storeStats.getBody().getOrDefault("total", 0).toString());
                activeStores = Long.parseLong(storeStats.getBody().getOrDefault("active", activeSubscriptions).toString());
                pendingStores = Long.parseLong(storeStats.getBody().getOrDefault("pending", 0).toString());
            }
        } catch (Exception e) {
            log.warn("Could not fetch store stats from user-org-service: {}", e.getMessage());
        }

        return DashboardSummaryDTO.builder()
                .totalStores(totalStores)
                .activeStores(activeStores)
                .pendingStores(pendingStores)
                .totalPlatformRevenue(totalRevenue)
                .build();
    }

    @Override
    public List<StoreRegistrationStatDTO> getLast7DayRegistrationStats() {
        try {
            ResponseEntity<List<StoreRegistrationStatDTO>> response = restTemplate.exchange(
                    userOrgServiceUrl + "/api/stores/registration-stats",
                    HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<StoreRegistrationStatDTO>>() {});
            return response.getBody() != null ? response.getBody() : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Could not fetch registration stats: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public StoreStatusDistributionDTO getStoreStatusDistribution() {
        // Default to returning subscription-based summary
        return StoreStatusDistributionDTO.builder()
                .count(subscriptionRepository.count())
                .build();
    }
}
