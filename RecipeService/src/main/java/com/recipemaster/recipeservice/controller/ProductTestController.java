package com.recipemaster.recipeservice.controller;

import com.recipemaster.entities.ProductEntity;
import com.recipemaster.recipeservice.service.ProductElasticService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductTestController {

    private final ProductElasticService productElasticService;

    @PostMapping("/find-or-create")
    public ProductEntity findOrCreate(@RequestBody ProductRequest request) {
        return productElasticService.findOrCreate(request.getName(), request.getUnit());
    }

    @Data
    public static class ProductRequest {
        private String name;
        private String unit;
    }
}
