package com.recipemaster.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Запрос на добавление записи в календарь питания")
public class CalendarEntryRequestDto {
    @Schema(description = "Дата приема пищи", example = "2026-05-02")
    private LocalDate date;

    @Schema(description = "Идентификатор продукта, если добавляется продукт", example = "3")
    private Long productId;

    @Schema(description = "Идентификатор рецепта, если добавляется рецепт", example = "12")
    private Long recipeId;

    @Schema(description = "Количество съеденного продукта или число порций рецепта", example = "1.5")
    private BigDecimal quantity;

    @Schema(description = "Нужно ли списать продукты из холодильника при добавлении рецепта в календарь", example = "true")
    private Boolean consumeFromFridge;
}
