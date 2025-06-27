package com.recipemaster.dto.responses;

import com.recipemaster.dto.RecipeDto;

public record RecipeMatchResponse(RecipeDto recipe, int matchedCount) {
}
