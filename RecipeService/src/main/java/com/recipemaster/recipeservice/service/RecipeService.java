package com.recipemaster.recipeservice.service;

import com.recipemaster.dto.RecipeDto;
import com.recipemaster.dto.RecipeInputDto;
import com.recipemaster.entities.IngredientEntity;
import com.recipemaster.entities.ProductEntity;
import com.recipemaster.entities.RecipeEntity;
import com.recipemaster.entities.UserEntity;
import com.recipemaster.recipeservice.repository.RecipeRepository;
import com.recipemaster.recipeservice.repository.UserRepository;
import com.recipemaster.recipeservice.repository.UsersProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
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
                            rd.getProductName(),
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
        Set<String> userProductNames = fetchUserProductNames(userId);
        if (userProductNames.isEmpty()) {
            return Collections.emptyList();
        }
        return buildTopRecipeMatches(userProductNames);
    }

    private Set<String> fetchUserProductNames(Long userId) {
        return usersProductRepository.findAllByUserId(userId).stream()
                .map(up -> up.getProduct().getName())
                .collect(Collectors.toSet());
    }

    private List<RecipeDto> buildTopRecipeMatches(Set<String> userProductNames) {
        return recipeRepository.findAll().stream()
                .map(recipe -> Map.entry(recipe, calculateMatchedCount(recipe, userProductNames)))
                .filter(recipe -> recipe.getValue()>0)
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(TOP_N)
                .map(recipe -> RecipeDto.fromEntity(recipe.getKey()))
                .toList();
    }

    private int calculateMatchedCount(RecipeEntity recipe, Set<String> userProductNames) {
        int total = recipe.getIngredients().size();
        long missing = recipe.getIngredients().stream()
                .map(i -> i.getProduct().getName())
                .filter(name -> !userProductNames.contains(name))
                .count();
        return total - (int) missing;
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
