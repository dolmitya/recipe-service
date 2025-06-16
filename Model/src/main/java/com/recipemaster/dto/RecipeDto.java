package com.recipemaster.dto;

import com.recipemaster.entities.RecipeEntity;
import lombok.Data;

import java.util.List;

@Data
public class RecipeDto {
    private Long id;
    private String title;
    private String description;
    private String category;
    private List<IngredientDto> ingredients;

    public static RecipeDto fromEntity(RecipeEntity entity) {
        RecipeDto dto = new RecipeDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setCategory(entity.getCategory());
        dto.setIngredients(entity.getIngredients().stream().map(IngredientDto::fromEntity).toList());
        return dto;
    }
}