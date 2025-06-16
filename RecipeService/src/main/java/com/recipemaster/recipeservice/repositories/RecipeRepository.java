package com.recipemaster.recipeservice.repositories;

import com.recipemaster.entities.RecipeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeRepository extends JpaRepository<RecipeEntity, Long> {
    List<RecipeEntity> findByCategory(String category);
}
