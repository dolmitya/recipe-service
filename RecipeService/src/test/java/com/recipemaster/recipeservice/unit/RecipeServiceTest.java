package com.recipemaster.recipeservice.unit;

import com.recipemaster.dto.IngredientDto;
import com.recipemaster.dto.RecipeDto;
import com.recipemaster.dto.RecipeInputDto;
import com.recipemaster.dto.responses.RecipeSuggestionDto;
import com.recipemaster.entities.IngredientEntity;
import com.recipemaster.entities.ProductEntity;
import com.recipemaster.entities.RecipeCategoryEntity;
import com.recipemaster.entities.RecipeEntity;
import com.recipemaster.entities.UserEntity;
import com.recipemaster.entities.UsersProductEntity;
import com.recipemaster.recipeservice.repository.RecipeRepository;
import com.recipemaster.recipeservice.repository.UserRepository;
import com.recipemaster.recipeservice.repository.UsersProductRepository;
import com.recipemaster.recipeservice.service.ProductElasticService;
import com.recipemaster.recipeservice.service.RecipeCategoryService;
import com.recipemaster.recipeservice.service.RecipeElasticService;
import com.recipemaster.recipeservice.service.RecipeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UsersProductRepository usersProductRepository;

    @Mock
    private ProductElasticService productElasticService;

    @Mock
    private RecipeElasticService recipeElasticService;

    @Mock
    private RecipeCategoryService recipeCategoryService;

    @InjectMocks
    private RecipeService recipeService;

    private ProductEntity rice;
    private RecipeEntity porridge;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        rice = product("Rice", "g", "3.30", "0.07", "0.01", "0.74");
        rice.setId(10L);
        porridge = recipe(1L, "Rice porridge", "breakfast", "Simple breakfast", new BigDecimal("2"), rice, new BigDecimal("100"));

        user = new UserEntity();
        user.setId(7L);
        user.setEmail("demo@recipe-service.local");
    }

    @Test
    void getAllRecipes_WhenCategoryAndQueryAbsent() {
        when(recipeRepository.findAll()).thenReturn(List.of(porridge));

        List<RecipeDto> result = recipeService.getAllRecipes(null, null);

        assertEquals(1, result.size());
        assertEquals("Rice porridge", result.getFirst().getTitle());
        assertEquals(new BigDecimal("330.00"), result.getFirst().getTotalCalories());
        verify(recipeRepository).findAll();
    }

    @Test
    void getAllRecipes_WhenCategoryProvided() {
        when(recipeCategoryService.normalizeName("Breakfast")).thenReturn("breakfast");
        when(recipeRepository.findByRecipeCategoryName("breakfast")).thenReturn(List.of(porridge));

        List<RecipeDto> result = recipeService.getAllRecipes("Breakfast", null);

        assertEquals(1, result.size());
        assertEquals("breakfast", result.getFirst().getCategory());
        verify(recipeRepository).findByRecipeCategoryName("breakfast");
        verify(recipeRepository, never()).findAll();
    }

    @Test
    void getAllRecipes_WhenQueryProvided() {
        when(recipeElasticService.searchRecipes("rice", null)).thenReturn(List.of(RecipeDto.fromEntity(porridge)));

        List<RecipeDto> result = recipeService.getAllRecipes(null, " rice ");

        assertEquals(1, result.size());
        assertEquals("Rice porridge", result.getFirst().getTitle());
        verify(recipeElasticService).searchRecipes("rice", null);
        verify(recipeRepository, never()).findAll();
    }

    @Test
    void getAllRecipes_WhenCategoryAndQueryProvided() {
        when(recipeCategoryService.normalizeName("Breakfast")).thenReturn("breakfast");
        when(recipeElasticService.searchRecipes("rice", "breakfast")).thenReturn(List.of(RecipeDto.fromEntity(porridge)));

        List<RecipeDto> result = recipeService.getAllRecipes("Breakfast", "rice");

        assertEquals(1, result.size());
        verify(recipeElasticService).searchRecipes("rice", "breakfast");
        verify(recipeRepository, never()).findByRecipeCategoryName(any());
    }

    @Test
    void suggestRecipes_WhenQueryProvided() {
        RecipeSuggestionDto suggestion = new RecipeSuggestionDto(1L, "Rice porridge");
        when(recipeElasticService.suggestRecipes("rice")).thenReturn(List.of(suggestion));

        List<RecipeSuggestionDto> result = recipeService.suggestRecipes("rice");

        assertEquals(1, result.size());
        assertEquals("Rice porridge", result.getFirst().getTitle());
        verify(recipeElasticService).suggestRecipes("rice");
    }

    @Test
    void addRecipe_WhenInputIsValid() {
        RecipeInputDto input = recipeInput("Rice porridge", "Breakfast", new BigDecimal("2"));
        when(productElasticService.findOrCreate(eq("Rice"), eq("g"), isNull())).thenReturn(rice);
        when(recipeCategoryService.findOrCreate("Breakfast")).thenReturn(new RecipeCategoryEntity("breakfast"));
        when(recipeRepository.save(any(RecipeEntity.class))).thenAnswer(invocation -> {
            RecipeEntity saved = invocation.getArgument(0);
            saved.setId(55L);
            return saved;
        });

        RecipeDto result = recipeService.addRecipe(input);

        assertEquals(55L, result.getId());
        assertEquals("Rice porridge", result.getTitle());
        assertEquals(1, result.getIngredients().size());
        assertEquals(new BigDecimal("330.00"), result.getTotalCalories());
        verify(recipeElasticService).indexRecipe(any(RecipeEntity.class));

        ArgumentCaptor<RecipeEntity> captor = ArgumentCaptor.forClass(RecipeEntity.class);
        verify(recipeRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getIngredients().size());
        assertEquals(rice, captor.getValue().getIngredients().getFirst().getProduct());
    }

    @Test
    void addRecipe_WhenRecipeIsNull() {
        assertThrows(NoSuchElementException.class, () -> recipeService.addRecipe(null));
        verify(recipeRepository, never()).save(any());
    }

    @Test
    void addRecipe_WhenIngredientsAbsent() {
        RecipeInputDto input = new RecipeInputDto();
        input.setTitle("Empty recipe");

        assertThrows(NoSuchElementException.class, () -> recipeService.addRecipe(input));
        verify(recipeRepository, never()).save(any());
    }

    @Test
    void addRecipe_WhenServingsNotPositive() {
        RecipeInputDto input = recipeInput("Bad recipe", "Breakfast", BigDecimal.ZERO);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> recipeService.addRecipe(input));

        assertEquals("Servings must be greater than zero", exception.getMessage());
        verify(recipeRepository, never()).save(any());
    }

    @Test
    void searchRecipesByUserProducts_WhenProductsAbsent() {
        when(usersProductRepository.findAllByUserId(user.getId())).thenReturn(List.of());

        List<RecipeDto> result = recipeService.searchRecipesByUserProducts(user.getId());

        assertTrue(result.isEmpty());
        verify(recipeRepository, never()).findAll();
    }

    @Test
    void searchRecipesByUserProducts_WhenRecipeMatches() {
        UsersProductEntity usersProduct = usersProduct(user, rice, new BigDecimal("120"));
        when(usersProductRepository.findAllByUserId(user.getId())).thenReturn(List.of(usersProduct));
        when(recipeRepository.findAll()).thenReturn(List.of(porridge));

        List<RecipeDto> result = recipeService.searchRecipesByUserProducts(user.getId());

        assertEquals(1, result.size());
        assertEquals("Rice porridge", result.getFirst().getTitle());
    }

    @Test
    void searchRecipesByUserProducts_PrefersNormalizedCoverageOverLongRecipe() {
        ProductEntity milk = product("Milk", "ml", "0.60", "0.03", "0.03", "0.05");
        milk.setId(12L);
        ProductEntity sugar = product("Sugar", "g", "3.87", "0.00", "0.00", "1.00");
        sugar.setId(13L);

        RecipeEntity simpleRecipe = recipe(2L, "Boiled rice", "Main", "Only rice", BigDecimal.ONE, rice, new BigDecimal("100"));
        RecipeEntity longRecipe = new RecipeEntity();
        longRecipe.setId(3L);
        longRecipe.setTitle("Rice dessert");
        longRecipe.setRecipeCategory(new RecipeCategoryEntity("dessert"));
        longRecipe.setDescription("Rice with milk and sugar");
        longRecipe.setServings(BigDecimal.ONE);

        IngredientEntity riceIngredient = ingredient(longRecipe, rice, new BigDecimal("100"));
        IngredientEntity milkIngredient = ingredient(longRecipe, milk, new BigDecimal("200"));
        IngredientEntity sugarIngredient = ingredient(longRecipe, sugar, new BigDecimal("50"));
        longRecipe.setIngredients(List.of(riceIngredient, milkIngredient, sugarIngredient));

        UsersProductEntity usersProduct = usersProduct(user, rice, new BigDecimal("100"));
        when(usersProductRepository.findAllByUserId(user.getId())).thenReturn(List.of(usersProduct));
        when(recipeRepository.findAll()).thenReturn(List.of(longRecipe, simpleRecipe));

        List<RecipeDto> result = recipeService.searchRecipesByUserProducts(user.getId());

        assertEquals(2, result.size());
        assertEquals("Boiled rice", result.getFirst().getTitle());
        assertEquals("Rice dessert", result.get(1).getTitle());
    }

    @Test
    void searchRecipesByUserProducts_WhenRecipeDoesNotMatch() {
        ProductEntity buckwheat = product("Buckwheat", "g", "3.43", "0.13", "0.03", "0.71");
        buckwheat.setId(11L);
        UsersProductEntity usersProduct = usersProduct(user, buckwheat, new BigDecimal("120"));
        when(usersProductRepository.findAllByUserId(user.getId())).thenReturn(List.of(usersProduct));
        when(recipeRepository.findAll()).thenReturn(List.of(porridge));

        List<RecipeDto> result = recipeService.searchRecipesByUserProducts(user.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void addRecipeToFavorites_WhenUserAndRecipeExist() {
        RecipeEntity favoriteRecipe = recipeWithoutIngredients(porridge.getId(), porridge.getTitle());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(recipeRepository.findById(favoriteRecipe.getId())).thenReturn(Optional.of(favoriteRecipe));

        RecipeDto result = recipeService.addRecipeToFavorites(user.getId(), favoriteRecipe.getId());

        assertEquals(1, user.getFavoriteRecipes().size());
        assertEquals(favoriteRecipe.getTitle(), user.getFavoriteRecipes().iterator().next().getTitle());
        assertEquals(favoriteRecipe.getTitle(), result.getTitle());
        verify(userRepository).save(user);
    }

    @Test
    void addRecipeToFavorites_WhenUserNotFound() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> recipeService.addRecipeToFavorites(user.getId(), porridge.getId()));
        verify(recipeRepository, never()).findById(any());
    }

    @Test
    void removeRecipeFromFavorites_WhenRecipeIsFavorite() {
        RecipeEntity favoriteRecipe = recipeWithoutIngredients(porridge.getId(), porridge.getTitle());
        user.getFavoriteRecipes().add(favoriteRecipe);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(recipeRepository.findById(favoriteRecipe.getId())).thenReturn(Optional.of(favoriteRecipe));

        recipeService.removeRecipeFromFavorites(user.getId(), favoriteRecipe.getId());

        assertTrue(user.getFavoriteRecipes().isEmpty());
        verify(userRepository).save(user);
    }

    @Test
    void removeRecipeFromFavorites_WhenRecipeNotFound() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(recipeRepository.findById(porridge.getId())).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> recipeService.removeRecipeFromFavorites(user.getId(), porridge.getId()));
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserFavorites_WhenFavoritesExist() {
        RecipeEntity favoriteRecipe = recipeWithoutIngredients(porridge.getId(), porridge.getTitle());
        user.getFavoriteRecipes().add(favoriteRecipe);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        List<RecipeDto> result = recipeService.getUserFavorites(user.getId());

        assertEquals(1, result.size());
        assertEquals("Rice porridge", result.getFirst().getTitle());
    }

    @Test
    void getUserFavorites_WhenUserNotFound() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> recipeService.getUserFavorites(user.getId()));
    }

    private RecipeInputDto recipeInput(String title, String category, BigDecimal servings) {
        IngredientDto ingredient = new IngredientDto();
        ingredient.setProductName("Rice");
        ingredient.setUnit("g");
        ingredient.setQuantity(new BigDecimal("100"));

        RecipeInputDto input = new RecipeInputDto();
        input.setTitle(title);
        input.setCategory(category);
        input.setDescription("Description");
        input.setServings(servings);
        input.setIngredients(List.of(ingredient));
        return input;
    }

    private RecipeEntity recipe(Long id,
                                String title,
                                String category,
                                String description,
                                BigDecimal servings,
                                ProductEntity product,
                                BigDecimal quantity) {
        RecipeEntity recipe = new RecipeEntity();
        recipe.setId(id);
        recipe.setTitle(title);
        recipe.setRecipeCategory(new RecipeCategoryEntity(category));
        recipe.setDescription(description);
        recipe.setServings(servings);

        IngredientEntity ingredient = new IngredientEntity();
        ingredient.setRecipe(recipe);
        ingredient.setProduct(product);
        ingredient.setQuantity(quantity);
        recipe.setIngredients(List.of(ingredient));
        return recipe;
    }

    private RecipeEntity recipeWithoutIngredients(Long id, String title) {
        RecipeEntity recipe = new RecipeEntity();
        recipe.setId(id);
        recipe.setTitle(title);
        recipe.setServings(BigDecimal.ONE);
        return recipe;
    }

    private IngredientEntity ingredient(RecipeEntity recipe, ProductEntity product, BigDecimal quantity) {
        IngredientEntity ingredient = new IngredientEntity();
        ingredient.setRecipe(recipe);
        ingredient.setProduct(product);
        ingredient.setQuantity(quantity);
        return ingredient;
    }

    private ProductEntity product(String name, String unit, String calories, String proteins, String fats, String carbs) {
        ProductEntity product = new ProductEntity();
        product.setName(name);
        product.setUnit(unit);
        product.setCaloriesPerUnit(new BigDecimal(calories));
        product.setProteinsPerUnit(new BigDecimal(proteins));
        product.setFatsPerUnit(new BigDecimal(fats));
        product.setCarbsPerUnit(new BigDecimal(carbs));
        return product;
    }

    private UsersProductEntity usersProduct(UserEntity user, ProductEntity product, BigDecimal quantity) {
        UsersProductEntity usersProduct = new UsersProductEntity();
        usersProduct.setUser(user);
        usersProduct.setProduct(product);
        usersProduct.setQuantity(quantity);
        return usersProduct;
    }
}
