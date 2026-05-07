package com.recipemaster.dto;

import com.recipemaster.entities.IngredientEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Optional;

@Data
@Schema(description = "Ингредиент рецепта")
public class IngredientDto {
    @Schema(description = "Название продукта", example = "картофель")
    private String productName;

    @Schema(description = "Количество ингредиента в рецепте", example = "3")
    private BigDecimal quantity;

    @Schema(description = "Единица измерения ингредиента", example = "шт")
    private String unit;

    @Schema(description = "Калорийность ингредиента в рамках рецепта", example = "210")
    private BigDecimal calories;

    @Schema(description = "Белки ингредиента в рамках рецепта", example = "12.5")
    private BigDecimal proteins;

    @Schema(description = "Жиры ингредиента в рамках рецепта", example = "5.1")
    private BigDecimal fats;

    @Schema(description = "Углеводы ингредиента в рамках рецепта", example = "18.0")
    private BigDecimal carbs;

    public static IngredientDto fromEntity(IngredientEntity entity) {
        IngredientDto dto = new IngredientDto();
        dto.setProductName(entity.getProduct().getName());
        dto.setQuantity(entity.getQuantity());
        dto.setUnit(entity.getProduct().getUnit());
        dto.setCalories(Optional.ofNullable(entity.getProduct().getCaloriesPerUnit())
                .orElse(BigDecimal.ZERO)
                .multiply(entity.getQuantity()));
        dto.setProteins(Optional.ofNullable(entity.getProduct().getProteinsPerUnit())
                .orElse(BigDecimal.ZERO)
                .multiply(entity.getQuantity()));
        dto.setFats(Optional.ofNullable(entity.getProduct().getFatsPerUnit())
                .orElse(BigDecimal.ZERO)
                .multiply(entity.getQuantity()));
        dto.setCarbs(Optional.ofNullable(entity.getProduct().getCarbsPerUnit())
                .orElse(BigDecimal.ZERO)
                .multiply(entity.getQuantity()));
        return dto;
    }
}
