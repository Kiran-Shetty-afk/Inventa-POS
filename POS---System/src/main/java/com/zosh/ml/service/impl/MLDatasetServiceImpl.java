package com.zosh.ml.service.impl;

import com.zosh.modal.Order;
import com.zosh.repository.OrderRepository;
import com.zosh.ml.service.MLDatasetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MLDatasetServiceImpl implements MLDatasetService {

    private final OrderRepository orderRepository;

    @Override
    public void exportDataset() {

        List<Order> orders = orderRepository.findAll();

        try (FileWriter writer = new FileWriter("fraud_dataset.csv")) {

            writer.write(
                    "totalAmount,paymentType,itemsCount,totalQuantity," +
                            "averageItemPrice,discountPercent,orderHour,fraud\n"
            );

            for (Order order : orders) {

                writer.write(
                        order.getTotalAmount() + "," +
                                order.getPaymentType() + "," +
                                order.getItemsCount() + "," +
                                order.getTotalQuantity() + "," +
                                order.getAverageItemPrice() + "," +
                                order.getDiscountPercent() + "," +
                                order.getOrderHour() + "," +
                                (order.getIsFraud() ? 1 : 0) +
                                "\n"
                );
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}