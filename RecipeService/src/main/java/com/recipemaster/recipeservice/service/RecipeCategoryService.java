package com.recipemaster.recipeservice.service;

import com.recipemaster.entities.RecipeCategoryEntity;
import com.recipemaster.recipeservice.repository.RecipeCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RecipeCategoryService {
    private final RecipeCategoryRepository recipeCategoryRepository;

    public RecipeCategoryEntity findOrCreate(String name) {
        String normalizedName = normalizeName(name);
        if (normalizedName == null) {
            return null;
        }

        return recipeCategoryRepository.findByName(normalizedName)
                .orElseGet(() -> createOrReadExisting(normalizedName));
    }

    private RecipeCategoryEntity createOrReadExisting(String normalizedName) {
        try {
            return recipeCategoryRepository.save(new RecipeCategoryEntity(normalizedName));
        } catch (DataIntegrityViolationException e) {
            return recipeCategoryRepository.findByName(normalizedName)
                    .orElseThrow(() -> e);
        }
    }

    public String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
