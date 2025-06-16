package com.recipemaster.dto;

import lombok.Data;

import java.util.List;

@Data
public class RecipeInputDto {
    private String title;
    private String description;
    private String category;
    private List<IngredientDto> ingredients;
}
