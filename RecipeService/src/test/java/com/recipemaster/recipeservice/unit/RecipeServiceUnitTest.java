package com.recipemaster.recipeservice.unit;

import com.recipemaster.dto.IngredientDto;
import com.recipemaster.dto.RecipeDto;
import com.recipemaster.dto.RecipeInputDto;
import com.recipemaster.dto.responses.RecipeMatchResponse;
import com.recipemaster.entities.ProductEntity;
import com.recipemaster.entities.RecipeEntity;
import com.recipemaster.entities.UserEntity;
import com.recipemaster.enums.ErrorMessage;
import com.recipemaster.recipeservice.repository.ProductRepository;
import com.recipemaster.recipeservice.repository.RecipeRepository;
import com.recipemaster.recipeservice.repository.UserRepository;
import com.recipemaster.recipeservice.repository.UsersProductRepository;
import com.recipemaster.recipeservice.service.ProductElasticService;
import com.recipemaster.recipeservice.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceUnitTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UsersProductRepository usersProductRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private RecipeService recipeService;

    @Mock
    private ProductElasticService productElasticService;

    @Test
    void testReturnOfAllRecipesWhenCategoryAbsent() {
        List<RecipeEntity> expectedRecipes = Arrays.asList(
                new RecipeEntity("Pasta", "Italian", "Make pasta"),
                new RecipeEntity( "Salad", "Healthy", "Make salad")
        );
        
        when(recipeRepository.findAll()).thenReturn(expectedRecipes);

        List<RecipeDto> result = recipeService.getAllRecipes(null);


        assertEquals(
                new HashSet<>(List.of("Pasta", "Salad")),
                new HashSet<>(result.stream().map(RecipeDto::getTitle).toList())
        );
        verify(recipeRepository).findAll();
        verify(recipeRepository, never()).findByCategory(any());
    }

    @Test
    void testReturnOfRecipesWhenCategoryProvided() {
        String category = "Italian";
        List<RecipeEntity> expectedRecipes = Collections.singletonList(
                new RecipeEntity("Pasta", category, "Make pasta")
        );

        when(recipeRepository.findByCategory(category)).thenReturn(expectedRecipes);

        List<RecipeDto> result = recipeService.getAllRecipes(category);

        assertEquals(1, result.size());
        assertEquals("Pasta", result.getFirst().getTitle());
        assertEquals(category, result.getFirst().getCategory());
        verify(recipeRepository).findByCategory(category);
        verify(recipeRepository, never()).findAll();
    }

    @Test
    void testAdditionOfRecipe() {
        RecipeInputDto inputDto = new RecipeInputDto();
        inputDto.setTitle("Test Recipe");
        inputDto.setCategory("Test");
        inputDto.setDescription("Test description");

        IngredientDto ingredientInput = new IngredientDto();
        ingredientInput.setProductName("Test Product");
        ingredientInput.setQuantity(new BigDecimal("1.0"));
        inputDto.setIngredients(Collections.singletonList(ingredientInput));

        ProductEntity product = new ProductEntity();
        product.setName("Test Product");
        product.setUnit("kg");

        when(productElasticService.findOrCreate(any(),any())).thenReturn(product);
        when(recipeRepository.save(any(RecipeEntity.class))).thenAnswer(invocation -> {
            RecipeEntity saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        RecipeDto result = recipeService.addRecipe(inputDto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Recipe", result.getTitle());
        assertEquals(1, result.getIngredients().size());
        assertEquals("Test Product", result.getIngredients().getFirst().getProductName());
        verify(productElasticService).findOrCreate(any(),any());
        verify(recipeRepository).save(any(RecipeEntity.class));
    }

    @Test
    void testSearchOfRecipesByUserProductsWhenUserProductsAbsent() {
        Long userId = 1L;

        when(usersProductRepository.findAllByUserId(userId)).thenReturn(Collections.emptyList());

        List<RecipeMatchResponse> result = recipeService.searchRecipesByUserProducts(userId);

        assertTrue(result.isEmpty());
        verify(usersProductRepository).findAllByUserId(userId);
        verify(recipeRepository, never()).findAll();
    }

    @Test
    void testAdditionOfRecipeToFavorites() {
        Long userId = 1L;
        Long recipeId = 1L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        RecipeEntity recipe = new RecipeEntity();
        recipe.setId(recipeId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));

        recipeService.addRecipeToFavorites(userId, recipeId);

        assertTrue(user.getFavoriteRecipes().contains(recipe));
        verify(userRepository).findById(userId);
        verify(recipeRepository).findById(recipeId);
        verify(userRepository).save(user);
    }

    @Test
    void testRemovalOfRecipeFromFavorites() {
        Long userId = 1L;
        Long recipeId = 1L;
        UserEntity user = new UserEntity();
        user.setId(userId);
        RecipeEntity recipe = new RecipeEntity();
        recipe.setId(recipeId);
        user.getFavoriteRecipes().add(recipe);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.of(recipe));

        recipeService.removeRecipeFromFavorites(userId, recipeId);

        assertFalse(user.getFavoriteRecipes().contains(recipe));
        verify(userRepository).findById(userId);
        verify(recipeRepository).findById(recipeId);
        verify(userRepository).save(user);
    }

    @Test
    void testReturnOfUserFavorites() {
        Long userId = 1L;
        UserEntity user = new UserEntity();
        user.setId(userId);

        RecipeEntity recipe1 = new RecipeEntity("Recipe 1", "Category", "Desc");
        RecipeEntity recipe2 = new RecipeEntity("Recipe 2", "Category", "Desc");

        user.getFavoriteRecipes().addAll(Arrays.asList(recipe1, recipe2));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        List<RecipeDto> result = recipeService.getUserFavorites(userId);

        assertEquals(2, result.size());
        assertEquals(
                new HashSet<>(List.of("Recipe 1", "Recipe 2")),
                new HashSet<>(result.stream().map(RecipeDto::getTitle).toList())
        );
        verify(userRepository).findById(userId);
    }

    @Test
    void testAdditionOfRecipeToFavoritesWhenUserNotFound() {
        Long userId = 1L;
        Long recipeId = 1L;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> recipeService.addRecipeToFavorites(userId, recipeId));
        verify(userRepository).findById(userId);
        verify(recipeRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRemovalOfRecipeFromFavoritesWhenRecipeNotFound() {
        Long userId = 1L;
        Long recipeId = 1L;
        UserEntity user = new UserEntity();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(recipeRepository.findById(recipeId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> recipeService.removeRecipeFromFavorites(userId, recipeId));
        verify(userRepository).findById(userId);
        verify(recipeRepository).findById(recipeId);
        verify(userRepository, never()).save(any());
    }
}
