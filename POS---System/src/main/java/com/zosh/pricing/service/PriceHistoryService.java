package com.zosh.pricing.service;

public interface PriceHistoryService {

    byte[] exportToExcel(Long productId);

}