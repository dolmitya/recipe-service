package com.recipemaster.recipeservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.recipemaster.RecipeElasticDocument;
import com.recipemaster.dto.RecipeDto;
import com.recipemaster.dto.responses.RecipeSuggestionDto;
import com.recipemaster.entities.RecipeEntity;
import com.recipemaster.recipeservice.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RecipeElasticService {
    private final RecipeRepository recipeRepository;
    private final ElasticsearchClient elasticsearchClient;

    public List<RecipeDto> searchRecipes(String query, String category) {
        List<Long> ids = searchRecipeIds(query, 50);
        Map<Long, RecipeEntity> recipesById = new LinkedHashMap<>();
        recipeRepository.findAllById(ids).forEach(recipe -> recipesById.put(recipe.getId(), recipe));

        return ids.stream()
                .map(recipesById::get)
                .filter(Objects::nonNull)
                .filter(recipe -> category == null || category.isBlank() || matchesCategory(recipe, category))
                .map(RecipeDto::fromEntity)
                .toList();
    }

    public List<RecipeSuggestionDto> suggestRecipes(String query) {
        List<Long> ids = searchRecipeIds(query, 10);
        Map<Long, RecipeEntity> recipesById = new LinkedHashMap<>();
        recipeRepository.findAllById(ids).forEach(recipe -> recipesById.put(recipe.getId(), recipe));

        return ids.stream()
                .map(recipesById::get)
                .filter(Objects::nonNull)
                .map(recipe -> new RecipeSuggestionDto(recipe.getId(), recipe.getTitle()))
                .toList();
    }

    public void indexRecipe(RecipeEntity recipe) {
        try {
            elasticsearchClient.index(i -> i
                    .index("recipes")
                    .id(recipe.getId().toString())
                    .document(toDocument(recipe))
            );
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при индексации рецепта в Elasticsearch", e);
        }
    }

    private List<Long> searchRecipeIds(String query, int size) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        try {
            SearchResponse<RecipeElasticDocument> response = elasticsearchClient.search(s -> s
                            .index("recipes")
                            .size(size)
                            .query(q -> q
                                    .matchPhrasePrefix(m -> m
                                            .field("title")
                                            .query(query.trim())
                                    )
                            ),
                    RecipeElasticDocument.class
            );

            return response.hits().hits().stream()
                    .map(hit -> hit.source())
                    .filter(Objects::nonNull)
                    .map(RecipeElasticDocument::getId)
                    .map(Long::valueOf)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при поиске рецептов в Elasticsearch", e);
        }
    }

    private RecipeElasticDocument toDocument(RecipeEntity recipe) {
        RecipeElasticDocument document = new RecipeElasticDocument();
        document.setId(recipe.getId().toString());
        document.setTitle(recipe.getTitle());
        document.setCategory(recipe.getCategory());
        return document;
    }

    private boolean matchesCategory(RecipeEntity recipe, String category) {
        return recipe.getCategory() != null
                && recipe.getCategory().equals(category.trim().toLowerCase(Locale.ROOT));
    }
}
