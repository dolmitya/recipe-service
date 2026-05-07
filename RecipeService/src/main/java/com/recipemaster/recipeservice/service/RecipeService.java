package com.recipemaster.recipeservice.service;

import com.recipemaster.dto.RecipeDto;
import com.recipemaster.dto.RecipeInputDto;
import com.recipemaster.dto.responses.RecipeSuggestionDto;
import com.recipemaster.entities.IngredientEntity;
import com.recipemaster.entities.ProductEntity;
import com.recipemaster.entities.RecipeEntity;
import com.recipemaster.entities.UserEntity;
import com.recipemaster.entities.UsersProductEntity;
import com.recipemaster.recipeservice.repository.RecipeRepository;
import com.recipemaster.recipeservice.repository.UserRepository;
import com.recipemaster.recipeservice.repository.UsersProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static com.recipemaster.recipeservice.mapper.RecipeMapper.recipeDTOToRecipeEntity;

@Service
@RequiredArgsConstructor
public class RecipeService {
    private static final int TOP_N = 5;

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final UsersProductRepository usersProductRepository;
    private final ProductElasticService productElasticService;
    private final RecipeElasticService recipeElasticService;

    public List<RecipeDto> getAllRecipes(String category) {
        return getAllRecipes(category, null);
    }

    public List<RecipeDto> getAllRecipes(String category, String query) {
        boolean hasCategory = category != null && !category.isBlank();
        boolean hasQuery = query != null && !query.isBlank();

        List<RecipeEntity> recipes;
        if (hasCategory && hasQuery) {
            return recipeElasticService.searchRecipes(query.trim(), category.trim());
        } else if (hasCategory) {
            recipes = recipeRepository.findByCategoryIgnoreCase(category.trim());
        } else if (hasQuery) {
            return recipeElasticService.searchRecipes(query.trim(), null);
        } else {
            recipes = recipeRepository.findAll();
        }

        return recipes.stream().map(RecipeDto::fromEntity).toList();
    }

    public List<RecipeSuggestionDto> suggestRecipes(String query) {
        return recipeElasticService.suggestRecipes(query);
    }

    public RecipeDto addRecipe(RecipeInputDto recipeDto) {
        if (recipeDto == null) {
            throw new NoSuchElementException("recipe cannot be null");
        }
        if (recipeDto.getIngredients() == null || recipeDto.getIngredients().isEmpty()) {
            throw new NoSuchElementException("Ingredients not provided");
        }
        if (recipeDto.getServings() != null && recipeDto.getServings().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Servings must be greater than zero");
        }

        RecipeEntity recipe = recipeDTOToRecipeEntity(recipeDto);
        List<IngredientEntity> ingredients = recipeDto.getIngredients().stream()
                .map(ingredientDto -> {
                    ProductEntity product = productElasticService.findOrCreate(
                            ingredientDto.getProductName().toLowerCase(),
                            ingredientDto.getUnit(),
                            null
                    );

                    IngredientEntity ingredient = new IngredientEntity();
                    ingredient.setRecipe(recipe);
                    ingredient.setQuantity(ingredientDto.getQuantity());
                    ingredient.setProduct(product);
                    return ingredient;
                })
                .toList();

        recipe.setIngredients(ingredients);
        RecipeEntity saved = recipeRepository.save(recipe);
        recipeElasticService.indexRecipe(saved);
        return RecipeDto.fromEntity(saved);
    }

    public List<RecipeDto> searchRecipesByUserProducts(Long userId) {
        Map<String, BigDecimal> userProductNames = fetchUserProductNames(userId);
        if (userProductNames.isEmpty()) {
            return Collections.emptyList();
        }
        return buildTopRecipeMatches(userProductNames);
    }

    private Map<String, BigDecimal> fetchUserProductNames(Long userId) {
        return usersProductRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(
                        up -> up.getProduct().getName(),
                        UsersProductEntity::getQuantity,
                        BigDecimal::add
                ));
    }

    private List<RecipeDto> buildTopRecipeMatches(Map<String, BigDecimal> userProductNames) {
        return recipeRepository.findAll().stream()
                .map(recipe -> Map.entry(recipe, calculateMatchedCount(recipe, userProductNames)))
                .filter(recipe -> recipe.getValue() > 0)
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(TOP_N)
                .map(recipe -> RecipeDto.fromEntity(recipe.getKey()))
                .toList();
    }

    private Double calculateMatchedCount(RecipeEntity recipe, Map<String, BigDecimal> userProductNames) {
        return recipe.getIngredients().stream()
                .mapToDouble(ingredient -> {
                    BigDecimal needed = ingredient.getQuantity();
                    BigDecimal available = userProductNames.getOrDefault(
                            ingredient.getProduct().getName(),
                            BigDecimal.ZERO
                    );
                    if (needed.compareTo(BigDecimal.ZERO) <= 0) {
                        return 0d;
                    }
                    double fraction = available.doubleValue() / needed.doubleValue();
                    return Math.min(1d, fraction);
                })
                .sum();
    }

    @Transactional
    public RecipeDto addRecipeToFavorites(Long userId, Long recipeId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        RecipeEntity recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new NoSuchElementException("Recipe not found"));
        user.getFavoriteRecipes().add(recipe);
        userRepository.save(user);
        return RecipeDto.fromEntity(recipe);
    }

    public void removeRecipeFromFavorites(Long userId, Long recipeId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        RecipeEntity recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new NoSuchElementException("Recipe not found"));
        user.getFavoriteRecipes().remove(recipe);
        userRepository.save(user);
    }

    public List<RecipeDto> getUserFavorites(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        return user.getFavoriteRecipes().stream().map(RecipeDto::fromEntity).toList();
    }
}
