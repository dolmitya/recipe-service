package com.recipemaster.recipeservice.controller;

import com.recipemaster.entities.ProductEntity;
import com.recipemaster.recipeservice.service.ProductElasticService;
import lombok.AllArgsConstructor;
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
    public ProductRequest findOrCreate(@RequestBody ProductRequest request) {
        ProductEntity product = productElasticService.findOrCreate(request.getName(), request.getUnit());
        return new ProductRequest(product.getName(), product.getUnit());
    }

    @Data
    @AllArgsConstructor
    public static class ProductRequest {
        private String name;
        private String unit;
    }
}
