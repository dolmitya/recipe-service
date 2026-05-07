package com.recipemaster.recipeservice.repository;

import com.recipemaster.entities.RecipeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecipeRepository extends JpaRepository<RecipeEntity, Long> {
    List<RecipeEntity> findByCategory(String category);
    List<RecipeEntity> findByCategoryIgnoreCase(String category);
    List<RecipeEntity> findByTitleContainingIgnoreCase(String title);
    List<RecipeEntity> findByCategoryIgnoreCaseAndTitleContainingIgnoreCase(String category, String title);
    List<RecipeEntity> findTop10ByTitleContainingIgnoreCaseOrderByTitleAsc(String title);
}
