package com.recipemaster.recipeservice.mapper;

import com.recipemaster.dto.RecipeInputDto;
import com.recipemaster.entities.RecipeEntity;

import java.math.BigDecimal;

public class RecipeMapper {
    public static RecipeEntity recipeDTOToRecipeEntity(RecipeInputDto recipeDto) {
        RecipeEntity recipe = new RecipeEntity();
        recipe.setDescription(recipeDto.getDescription());
        recipe.setTitle(recipeDto.getTitle());
        recipe.setServings(recipeDto.getServings() != null && recipeDto.getServings().compareTo(BigDecimal.ZERO) > 0
                ? recipeDto.getServings()
                : BigDecimal.ONE);
        return recipe;
    }
}
