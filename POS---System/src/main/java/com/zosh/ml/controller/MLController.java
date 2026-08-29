//package com.zosh.ml.controller;
//
//import com.zosh.ml.service.MLDatasetService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/ml")
//@RequiredArgsConstructor
//public class MLController {
//
//    private final MLDatasetService mlDatasetService;
//
//    @PostMapping("/export")
//    public String exportDataset() {
//
//        mlDatasetService.exportDataset();
//
//        return "Dataset exported successfully.";
//
//    }
//
//}
package com.zosh.ml.controller;

import com.zosh.ml.dto.FraudPredictionResponse;
import com.zosh.ml.service.FraudPredictionService;
import com.zosh.modal.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MLController {

    private final FraudPredictionService fraudPredictionService;

    @GetMapping("/api/ml/test")
    public FraudPredictionResponse test() {

        Order order = new Order();

        order.setTotalAmount(9000.0);
        order.setPaymentType(com.zosh.domain.PaymentType.CASH);
        order.setItemsCount(18);
        order.setTotalQuantity(55);
        order.setAverageItemPrice(163.0);
        order.setDiscountPercent(50.0);
        order.setOrderHour(2);

        return fraudPredictionService.predict(order);
    }
}