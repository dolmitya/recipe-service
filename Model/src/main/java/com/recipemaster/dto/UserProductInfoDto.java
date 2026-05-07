package com.recipemaster.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Продукт пользователя в холодильнике")
public class UserProductInfoDto {
    @Schema(description = "Идентификатор продукта", example = "1")
    private Long id;

    @Schema(description = "Название продукта", example = "молоко")
    private String name;

    @Schema(description = "Количество продукта у пользователя", example = "2.0")
    private BigDecimal quantity;

    @Schema(description = "Единица измерения", example = "л")
    private String unit;

    @Schema(description = "Калории за одну единицу продукта", example = "42.0")
    private BigDecimal caloriesPerUnit;

    @Schema(description = "Белки за одну единицу продукта", example = "3.4")
    private BigDecimal proteinsPerUnit;

    @Schema(description = "Жиры за одну единицу продукта", example = "1.2")
    private BigDecimal fatsPerUnit;

    @Schema(description = "Углеводы за одну единицу продукта", example = "5.0")
    private BigDecimal carbsPerUnit;

    @Schema(description = "Суммарная калорийность всего количества продукта", example = "84.0")
    private BigDecimal totalCalories;

    @Schema(description = "Суммарное количество белков во всем продукте", example = "6.8")
    private BigDecimal totalProteins;

    @Schema(description = "Суммарное количество жиров во всем продукте", example = "2.4")
    private BigDecimal totalFats;

    @Schema(description = "Суммарное количество углеводов во всем продукте", example = "10.0")
    private BigDecimal totalCarbs;

    public UserProductInfoDto(Long id, String name, BigDecimal quantity, String unit) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
    }
}
