package com.recipemaster.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Календарь питания за конкретный день")
public class CalendarDayDto {
    @Schema(description = "Дата", example = "2026-05-02")
    private LocalDate date;

    @Schema(description = "Общая калорийность за день", example = "1450")
    private BigDecimal totalCalories;

    @Schema(description = "Общее количество белков за день", example = "96")
    private BigDecimal totalProteins;

    @Schema(description = "Общее количество жиров за день", example = "54")
    private BigDecimal totalFats;

    @Schema(description = "Общее количество углеводов за день", example = "140")
    private BigDecimal totalCarbs;

    @Schema(description = "Список записей за день")
    private List<CalendarEntryDto> entries;
}
