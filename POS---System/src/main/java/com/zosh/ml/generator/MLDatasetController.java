package com.zosh.ml.controller;

import com.zosh.ml.generator.DatasetGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
public class MLDatasetController {

    private final DatasetGeneratorService datasetGeneratorService;

    @PostMapping("/generate")
    public String generate(
            @RequestParam(defaultValue = "1000") int count
    ) {

        datasetGeneratorService.generateDataset(count);

        return count + " records generated.";

    }

}