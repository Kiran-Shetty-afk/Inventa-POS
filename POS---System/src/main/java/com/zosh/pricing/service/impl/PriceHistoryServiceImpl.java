package com.zosh.pricing.service.impl;

import com.zosh.pricing.model.PriceHistory;
import com.zosh.pricing.repository.PriceHistoryRepository;
import com.zosh.pricing.service.PriceHistoryService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceHistoryServiceImpl implements PriceHistoryService {

    private final PriceHistoryRepository priceHistoryRepository;

    @Override
    public byte[] exportToExcel(Long productId) {

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Price History");

            Row header = sheet.createRow(0);

            header.createCell(0).setCellValue("Product");
            header.createCell(1).setCellValue("Old Price");
            header.createCell(2).setCellValue("New Price");
            header.createCell(3).setCellValue("Difference");
            header.createCell(4).setCellValue("Reason");
            header.createCell(5).setCellValue("Changed At");

            List<PriceHistory> history =
                    priceHistoryRepository.findByProductIdOrderByChangedAtDesc(productId);
            System.out.println("Product ID: " + productId);
            System.out.println("Records found: " + history.size());

            int rowNum = 1;

            for (PriceHistory item : history) {
                System.out.println(item.getProduct().getName());

                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(item.getProduct().getName());

                row.createCell(1).setCellValue(item.getOldPrice());

                row.createCell(2).setCellValue(item.getNewPrice());

                row.createCell(3).setCellValue(item.getPriceDifference());

                row.createCell(4).setCellValue(item.getReason());

                row.createCell(5).setCellValue(
                        item.getChangedAt().toString()
                );

            }

            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out =
                    new ByteArrayOutputStream();

            workbook.write(out);

            return out.toByteArray();

        } catch (Exception e) {

            throw new RuntimeException("Failed to export Excel", e);

        }

    }

}