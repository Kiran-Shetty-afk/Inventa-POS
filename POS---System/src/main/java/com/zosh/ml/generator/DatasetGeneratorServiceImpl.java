package com.zosh.ml.generator;

import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

@Service
public class DatasetGeneratorServiceImpl implements DatasetGeneratorService {

    private final Random random = new Random();

    @Override
    public void generateDataset(int count) {

        try (FileWriter writer = new FileWriter("fraud_dataset.csv")) {

            writer.write(
                    "totalAmount,paymentType,itemsCount,totalQuantity,averageItemPrice,discountPercent,orderHour,fraud\n"
            );

            for (int i = 0; i < count; i++) {

                boolean fraud = random.nextDouble() < 0.20; // 20% fraud

                double totalAmount;
                String paymentType;
                int itemsCount;
                int totalQuantity;
                double averageItemPrice;
                double discountPercent;
                int orderHour;

                if (fraud) {

                    totalAmount = randomBetween(5000, 15000);

                    paymentType = "CASH";

                    itemsCount = random.nextInt(15) + 10;

                    totalQuantity = random.nextInt(50) + 30;

                    averageItemPrice = totalAmount / totalQuantity;

                    discountPercent = random.nextBoolean()
                            ? randomBetween(40, 70)
                            : 0;

                    orderHour = random.nextBoolean()
                            ? random.nextInt(5)
                            : random.nextInt(24);

                } else {

                    totalAmount = randomBetween(100, 3000);

                    String[] payments = {"CARD", "UPI", "CASH"};

                    paymentType = payments[random.nextInt(payments.length)];

                    itemsCount = random.nextInt(5) + 1;

                    totalQuantity = random.nextInt(10) + 1;

                    averageItemPrice = totalAmount / totalQuantity;

                    discountPercent = randomBetween(0, 20);

                    orderHour = random.nextInt(24);

                }

                writer.write(
                        totalAmount + "," +
                                paymentType + "," +
                                itemsCount + "," +
                                totalQuantity + "," +
                                averageItemPrice + "," +
                                discountPercent + "," +
                                orderHour + "," +
                                (fraud ? 1 : 0) +
                                "\n"
                );

            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private double randomBetween(double min, double max) {
        return min + (max - min) * random.nextDouble();
    }
}