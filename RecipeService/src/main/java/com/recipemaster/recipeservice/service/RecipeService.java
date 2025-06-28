package com.recipemaster.recipeservice.service;

import com.recipemaster.dto.RecipeDto;
import com.recipemaster.dto.RecipeInputDto;
import com.recipemaster.entities.*;
import com.recipemaster.recipeservice.repository.RecipeRepository;
import com.recipemaster.recipeservice.repository.UserRepository;
import com.recipemaster.recipeservice.repository.UsersProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.recipemaster.recipeservice.mapper.RecipeMapper.recipeDTOToRecipeEntity;

@Service
@RequiredArgsConstructor
public class RecipeService {
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final UsersProductRepository usersProductRepository;
    private final ProductElasticService productElasticService;
    private static final int TOP_N = 5;

    public List<RecipeDto> getAllRecipes(String category) {
        List<RecipeEntity> recipes = (category == null || category.isEmpty())
                ? recipeRepository.findAll()
                : recipeRepository.findByCategory(category);
        return recipes.stream().map(RecipeDto::fromEntity).toList();
    }

    public RecipeDto addRecipe(RecipeInputDto recipeDto) {
        if (recipeDto == null) {
            throw new NoSuchElementException("recipe cannot be null");
        }
        if (recipeDto.getIngredients() == null || recipeDto.getIngredients().isEmpty()) {
            throw new NoSuchElementException("Ingredients not provided");
        }
        RecipeEntity recipe = recipeDTOToRecipeEntity(recipeDto);
        List<IngredientEntity> ingredients = recipeDto.getIngredients().stream()
                .map(rd -> {
                    ProductEntity product = productElasticService.findOrCreate(
                            rd.getProductName().toLowerCase(),
                            rd.getUnit()
                    );

                    IngredientEntity ing = new IngredientEntity();
                    ing.setRecipe(recipe);
                    ing.setQuantity(rd.getQuantity());
                    ing.setProduct(product);
                    return ing;
                })
                .toList();

        recipe.setIngredients(ingredients);
        RecipeEntity saved = recipeRepository.save(recipe);
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
                .filter(recipe -> recipe.getValue()>0)
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(TOP_N)
                .map(recipe -> RecipeDto.fromEntity(recipe.getKey()))
                .toList();
    }

    private Double calculateMatchedCount(RecipeEntity recipe, Map<String, BigDecimal> userProductNames) {
        return recipe.getIngredients().stream()
                .mapToDouble(i -> {
                    BigDecimal needed = i.getQuantity();
                    BigDecimal available = userProductNames.getOrDefault(
                            i.getProduct().getName(),
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
