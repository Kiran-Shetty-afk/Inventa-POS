package com.zosh.fraud.service;

import com.zosh.modal.Order;

public interface FraudDetectionService {

    void detectFraud(Order order);

}