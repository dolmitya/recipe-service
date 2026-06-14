package com.recipemaster.recipeservice.repository;

import com.recipemaster.entities.RecipeCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecipeCategoryRepository extends JpaRepository<RecipeCategoryEntity, Long> {
    Optional<RecipeCategoryEntity> findByName(String name);
}
