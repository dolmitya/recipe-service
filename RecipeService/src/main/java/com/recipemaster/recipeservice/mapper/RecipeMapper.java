package com.recipemaster.recipeservice.mapper;

import com.recipemaster.dto.RecipeInputDto;
import com.recipemaster.entities.RecipeEntity;

public class RecipeMapper {
    public static RecipeEntity recipeDTOToRecipeEntity(RecipeInputDto recipeDto) {
        RecipeEntity recipe = new RecipeEntity();
        recipe.setCategory(recipeDto.getCategory());
        recipe.setDescription(recipeDto.getDescription());
        recipe.setTitle(recipeDto.getTitle());
        return recipe;
    }
}
