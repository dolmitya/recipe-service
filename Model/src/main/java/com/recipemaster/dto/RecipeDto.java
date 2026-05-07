package com.recipemaster.dto;

import com.recipemaster.entities.RecipeEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Data
@Schema(description = "Рецепт")
public class RecipeDto {
    @Schema(description = "Идентификатор рецепта", example = "10")
    private Long id;

    @Schema(description = "Название рецепта", example = "Борщ")
    private String title;

    @Schema(description = "Описание рецепта", example = "Классический борщ со свеклой")
    private String description;

    @Schema(description = "Категория рецепта", example = "Обед")
    private String category;

    @Schema(description = "Количество порций", example = "4")
    private BigDecimal servings;

    @Schema(description = "Полная калорийность всего рецепта", example = "640")
    private BigDecimal totalCalories;

    @Schema(description = "Калорийность одной порции", example = "160")
    private BigDecimal caloriesPerServing;

    @Schema(description = "Общее количество белков в рецепте", example = "32")
    private BigDecimal totalProteins;

    @Schema(description = "Общее количество жиров в рецепте", example = "18")
    private BigDecimal totalFats;

    @Schema(description = "Общее количество углеводов в рецепте", example = "74")
    private BigDecimal totalCarbs;

    @Schema(description = "Белки на одну порцию", example = "8")
    private BigDecimal proteinsPerServing;

    @Schema(description = "Жиры на одну порцию", example = "4.5")
    private BigDecimal fatsPerServing;

    @Schema(description = "Углеводы на одну порцию", example = "18.5")
    private BigDecimal carbsPerServing;

    @Schema(description = "Состав рецепта")
    private List<IngredientDto> ingredients;

    public static RecipeDto fromEntity(RecipeEntity entity) {
        RecipeDto dto = new RecipeDto();
        BigDecimal totalCalories = entity.getIngredients().stream()
                .map(ingredient -> Optional.ofNullable(ingredient.getProduct().getCaloriesPerUnit())
                        .orElse(BigDecimal.ZERO)
                        .multiply(ingredient.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProteins = entity.getIngredients().stream()
                .map(ingredient -> Optional.ofNullable(ingredient.getProduct().getProteinsPerUnit())
                        .orElse(BigDecimal.ZERO)
                        .multiply(ingredient.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFats = entity.getIngredients().stream()
                .map(ingredient -> Optional.ofNullable(ingredient.getProduct().getFatsPerUnit())
                        .orElse(BigDecimal.ZERO)
                        .multiply(ingredient.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCarbs = entity.getIngredients().stream()
                .map(ingredient -> Optional.ofNullable(ingredient.getProduct().getCarbsPerUnit())
                        .orElse(BigDecimal.ZERO)
                        .multiply(ingredient.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal servings = Optional.ofNullable(entity.getServings())
                .filter(value -> value.compareTo(BigDecimal.ZERO) > 0)
                .orElse(BigDecimal.ONE);

        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setCategory(entity.getCategory());
        dto.setServings(servings);
        dto.setTotalCalories(totalCalories);
        dto.setCaloriesPerServing(totalCalories.divide(servings, 2, RoundingMode.HALF_UP));
        dto.setTotalProteins(totalProteins);
        dto.setTotalFats(totalFats);
        dto.setTotalCarbs(totalCarbs);
        dto.setProteinsPerServing(totalProteins.divide(servings, 2, RoundingMode.HALF_UP));
        dto.setFatsPerServing(totalFats.divide(servings, 2, RoundingMode.HALF_UP));
        dto.setCarbsPerServing(totalCarbs.divide(servings, 2, RoundingMode.HALF_UP));
        dto.setIngredients(entity.getIngredients().stream().map(IngredientDto::fromEntity).toList());
        return dto;
    }
}
