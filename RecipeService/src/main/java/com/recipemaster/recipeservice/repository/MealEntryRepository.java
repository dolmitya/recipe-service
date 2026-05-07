package com.recipemaster.recipeservice.repository;

import com.recipemaster.entities.MealEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MealEntryRepository extends JpaRepository<MealEntryEntity, Long> {
    List<MealEntryEntity> findAllByUserIdAndConsumedOnOrderByIdDesc(Long userId, LocalDate consumedOn);
    Optional<MealEntryEntity> findByIdAndUserId(Long id, Long userId);
}
