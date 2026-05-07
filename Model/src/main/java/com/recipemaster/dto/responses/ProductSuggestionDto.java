package com.recipemaster.dto.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Schema(description = "Подсказка по названию продукта")
public class ProductSuggestionDto {
    @Schema(description = "Название найденного продукта", example = "Помидор")
    private String name;

    @Schema(description = "Единица измерения продукта", example = "шт")
    private String unit;

    @Schema(description = "Калорийность за одну единицу", example = "52.0")
    private BigDecimal caloriesPerUnit;

    @Schema(description = "Белки за одну единицу", example = "1.1")
    private BigDecimal proteinsPerUnit;

    @Schema(description = "Жиры за одну единицу", example = "0.2")
    private BigDecimal fatsPerUnit;

    @Schema(description = "Углеводы за одну единицу", example = "3.8")
    private BigDecimal carbsPerUnit;
}
