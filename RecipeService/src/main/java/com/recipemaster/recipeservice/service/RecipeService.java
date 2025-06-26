package com.recipemaster.recipeservice.service;

import com.recipemaster.dto.RecipeDto;
import com.recipemaster.dto.RecipeInputDto;
import com.recipemaster.entities.IngredientEntity;
import com.recipemaster.entities.RecipeEntity;
import com.recipemaster.entities.UserEntity;
import com.recipemaster.entities.UsersProductEntity;
import com.recipemaster.enums.ErrorMessage;
import com.recipemaster.recipeservice.repository.ProductRepository;
import com.recipemaster.recipeservice.repository.RecipeRepository;
import com.recipemaster.recipeservice.repository.UserRepository;
import com.recipemaster.recipeservice.repository.UsersProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.recipemaster.recipeservice.mapper.RecipeMapper.recipeDTOToRecipeEntity;

@Service
@RequiredArgsConstructor
public class RecipeService {
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final UsersProductRepository usersProductRepository;
    private final ProductRepository productRepository;

    public List<RecipeDto> getAllRecipes(String category) {
        List<RecipeEntity> recipes = (category == null || category.isEmpty())
                ? recipeRepository.findAll()
                : recipeRepository.findByCategory(category);
        return recipes.stream().map(RecipeDto::fromEntity).toList();
    }

    public RecipeDto addRecipe(RecipeInputDto recipeDto) {
        // TODO: Добавить сохранение рецепта  здесь везде тоже нужен elastic, а  его пока нет)
        RecipeEntity recipe = recipeDTOToRecipeEntity(recipeDto);
        recipe.setIngredients(recipeDto.getIngredients().stream().map(rd -> {
            IngredientEntity ingredient = new IngredientEntity();
            ingredient.setRecipe(recipe);
            ingredient.setQuantity(rd.getQuantity());
            ingredient.setProduct(productRepository.findByName(rd.getProductName())
                    .orElseThrow(() -> new NoSuchElementException(ErrorMessage.INCORRECT_PRODUCT_NAME.getMessage())));
            return ingredient;
        }).toList());
        return RecipeDto.fromEntity(recipeRepository.save(recipe)); // TODO: Вернуть сохранённый рецепт
    }

    public List<RecipeDto> searchRecipesByUserProducts(Long userId) {
        List<UsersProductEntity> userProducts = usersProductRepository.findAllByUserId(userId);
        if (userProducts.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> userProductNames = userProducts.stream()
                .map(up -> up.getProduct().getName())
                .toList();

        List<RecipeEntity> allRecipes = recipeRepository.findAll();

        //TODO: добавить поиск рецептов по продуктам

        return new LinkedList<>();
    }

    public void addRecipeToFavorites(Long userId, Long recipeId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        RecipeEntity recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new NoSuchElementException("Recipe not found"));
        user.getFavoriteRecipes().add(recipe);
        userRepository.save(user);
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
