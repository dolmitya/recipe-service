package com.recipemaster.recipeservice.mappers;

import com.recipemaster.dto.RecipeDto;
import com.recipemaster.dto.RecipeInputDto;
import com.recipemaster.entities.IngredientEntity;
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
