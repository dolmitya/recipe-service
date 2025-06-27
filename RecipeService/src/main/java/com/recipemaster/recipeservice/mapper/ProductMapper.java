package com.recipemaster.recipeservice.mapper;

import com.recipemaster.dto.UserProductInfoDto;
import com.recipemaster.entities.ProductEntity;

public class ProductMapper {
    public static ProductEntity productDTOToProductEntity(UserProductInfoDto productInputDto) {
        ProductEntity product = new ProductEntity();
        product.setName(productInputDto.getName());
        product.setUnit(productInputDto.getUnit());
        return product;
    }
}
