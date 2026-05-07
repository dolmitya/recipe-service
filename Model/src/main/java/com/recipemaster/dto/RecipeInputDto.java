package com.recipemaster.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "Данные для создания рецепта")
public class RecipeInputDto {
    @Schema(description = "Название рецепта", example = "Картофельное пюре")
    private String title;
    @Schema(description = "Описание рецепта", example = "Простой гарнир на каждый день")
    private String description;
    @Schema(description = "Категория рецепта", example = "Ужин")
    private String category;
    @Schema(description = "Количество порций, которое дает рецепт", example = "4")
    private BigDecimal servings;
    @Schema(description = "Список ингредиентов рецепта")
    private List<IngredientDto> ingredients;
}
