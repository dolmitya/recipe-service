package com.recipemaster.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запись в календаре питания")
public class CalendarEntryDto {
    @Schema(description = "Идентификатор записи", example = "15")
    private Long id;

    @Schema(description = "Тип записи: PRODUCT или RECIPE", example = "PRODUCT")
    private String entryType;

    @Schema(description = "Идентификатор продукта или рецепта", example = "4")
    private Long referenceId;

    @Schema(description = "Название продукта или рецепта", example = "гречка")
    private String name;

    @Schema(description = "Сколько было съедено", example = "2")
    private BigDecimal quantity;

    @Schema(description = "Единица измерения или порции", example = "порц.")
    private String unitLabel;

    @Schema(description = "Калорийность этой записи", example = "180")
    private BigDecimal calories;

    @Schema(description = "Белки в этой записи", example = "12.5")
    private BigDecimal proteins;

    @Schema(description = "Жиры в этой записи", example = "5.1")
    private BigDecimal fats;

    @Schema(description = "Углеводы в этой записи", example = "18.0")
    private BigDecimal carbs;
}
