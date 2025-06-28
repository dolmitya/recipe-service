package com.recipemaster.recipeservice.mapper;

import com.recipemaster.dto.RecipeDto;
import com.recipemaster.dto.responses.RecipeMatchResponse;
import com.recipemaster.entities.RecipeEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public final class RecipeMatchMapper {

    public static RecipeMatchResponse toResponse(Map.Entry<RecipeEntity, Integer> entry) {
        RecipeDto dto = RecipeDto.fromEntity(entry.getKey());
        return new RecipeMatchResponse(dto, entry.getValue());
    }
}
