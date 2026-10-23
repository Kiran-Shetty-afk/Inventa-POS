package com.zosh.service;

import com.zosh.payload.AdminAnalysis.DashboardSummaryDTO;
import com.zosh.payload.AdminAnalysis.StoreRegistrationStatDTO;
import com.zosh.payload.AdminAnalysis.StoreStatusDistributionDTO;

import java.util.List;

public interface AdminDashboardService {
    DashboardSummaryDTO getDashboardSummary(String authorization);
    List<StoreRegistrationStatDTO> getLast7DayRegistrationStats(String authorization);
    StoreStatusDistributionDTO getStoreStatusDistribution(String authorization);
}
