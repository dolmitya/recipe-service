package com.recipemaster.recipeservice.repository;

import com.recipemaster.entities.RecipeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeRepository extends JpaRepository<RecipeEntity, Long> {
    List<RecipeEntity> findByTitleContainingIgnoreCase(String title);
    List<RecipeEntity> findByRecipeCategoryName(String category);
    List<RecipeEntity> findTop10ByTitleContainingIgnoreCaseOrderByTitleAsc(String title);
}
