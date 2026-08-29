package com.zosh.analytics.service.impl;

import com.zosh.analytics.dto.AnalyticsDashboardDTO;
import com.zosh.analytics.service.AnalyticsService;
import com.zosh.modal.Order;
import com.zosh.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final OrderRepository orderRepository;

    @Override
    public AnalyticsDashboardDTO getDashboard() {

        List<Order> orders = orderRepository.findAll();

        long totalOrders = orders.size();

        double totalRevenue = orders.stream()
                .mapToDouble(Order::getTotalAmount)
                .sum();

        double averageOrderValue =
                totalOrders == 0 ? 0 : totalRevenue / totalOrders;

        double highestOrderValue = orders.stream()
                .mapToDouble(Order::getTotalAmount)
                .max()
                .orElse(0);

        return AnalyticsDashboardDTO.builder()
                .totalOrders(totalOrders)
                .totalRevenue(totalRevenue)
                .averageOrderValue(averageOrderValue)
                .highestOrderValue(highestOrderValue)
                .build();
    }
}