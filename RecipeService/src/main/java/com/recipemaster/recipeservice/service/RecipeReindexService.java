package com.recipemaster.recipeservice.service;

import com.recipemaster.entities.RecipeEntity;
import com.recipemaster.recipeservice.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeReindexService {
    private final RecipeRepository recipeRepository;
    private final RecipeElasticService recipeElasticService;

    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void reindexAll() {
        log.info("Recipe reindex started");

        try {
            Iterable<RecipeEntity> recipes = recipeRepository.findAll();
            recipes.forEach(recipeElasticService::indexRecipe);
        } catch (Exception e) {
            log.error("Recipe reindex failed", e);
        }

        log.info("Recipe reindex finished");
    }
}
