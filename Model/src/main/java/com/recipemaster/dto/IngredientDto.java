package com.recipemaster.dto;

import com.recipemaster.entities.IngredientEntity;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class IngredientDto {
    private String productName;
    private BigDecimal quantity;
    private String unit;

    public static IngredientDto fromEntity(IngredientEntity entity) {
        IngredientDto dto = new IngredientDto();
        dto.setProductName(entity.getProduct().getName());
        dto.setQuantity(entity.getQuantity());
        dto.setUnit(entity.getProduct().getUnit());
        return dto;
    }
}
