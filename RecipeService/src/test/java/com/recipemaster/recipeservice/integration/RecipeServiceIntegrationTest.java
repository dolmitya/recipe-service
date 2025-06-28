package com.recipemaster.recipeservice.integration;

import com.recipemaster.dto.IngredientDto;
import com.recipemaster.dto.RecipeDto;
import com.recipemaster.dto.RecipeInputDto;
import com.recipemaster.dto.responses.RecipeMatchResponse;
import com.recipemaster.entities.IngredientEntity;
import com.recipemaster.entities.ProductEntity;
import com.recipemaster.entities.RecipeEntity;
import com.recipemaster.entities.UserEntity;
import com.recipemaster.entities.UsersProductEntity;
import com.recipemaster.recipeservice.repository.ProductRepository;
import com.recipemaster.recipeservice.repository.RecipeRepository;
import com.recipemaster.recipeservice.repository.UserRepository;
import com.recipemaster.recipeservice.repository.UsersProductRepository;
import com.recipemaster.recipeservice.service.RecipeService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@Transactional
class RecipeServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "true");
    }

    @Autowired
    private RecipeService recipeService;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UsersProductRepository usersProductRepository;

    @Autowired
    private ProductRepository productRepository;

    private UserEntity testUser;
    private ProductEntity testProduct;
    private RecipeEntity testRecipe;

    @BeforeEach
    void setUp() {

        usersProductRepository.deleteAll();
        recipeRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new UserEntity("c@c.cc", "123", "testUser");
        testUser = userRepository.save(testUser);

        testProduct = new ProductEntity();
        testProduct.setName("Test Product");
        testProduct.setUnit("gram");
        testProduct = productRepository.save(testProduct);

        testRecipe = new RecipeEntity();
        testRecipe.setTitle("Test Recipe");
        testRecipe.setDescription("Test Description");
        testRecipe.setCategory("Test Category");

        IngredientEntity ingredient = new IngredientEntity();
        ingredient.setProduct(testProduct);
        ingredient.setQuantity(new BigDecimal(2));
        ingredient.setRecipe(testRecipe);
        testRecipe.setIngredients(List.of(ingredient));

        testRecipe = recipeRepository.save(testRecipe);
    }

    @Test
    void testReturnOfAllRecipesGivingNoCategory() {

        List<RecipeDto> result = recipeService.getAllRecipes(null);

        assertEquals(1, result.size());
        assertEquals("Test Recipe", result.getFirst().getTitle());
    }

    @Test
    void testReturnOfAllRecipesWithCategory() {

        List<RecipeDto> result = recipeService.getAllRecipes("Test Category");

        assertEquals(1, result.size());
        assertEquals("Test Recipe", result.getFirst().getTitle());
    }

    @Test
    void testSuccessfulAdditionOfRecipe() {

        RecipeInputDto inputDto = new RecipeInputDto();
        inputDto.setTitle("New Recipe");
        inputDto.setDescription("Test Description");
        inputDto.setCategory("New Category");

        IngredientDto ingredientInput = new IngredientDto();
        ingredientInput.setProductName("Test Product");
        ingredientInput.setQuantity(new BigDecimal(2));
        inputDto.setIngredients(List.of(ingredientInput));

        RecipeDto result = recipeService.addRecipe(inputDto);

        Assertions.assertNotNull(result.getId());
        assertEquals("New Recipe", result.getTitle());
        assertEquals(1, result.getIngredients().size());
        assertEquals("Test Product", result.getIngredients().getFirst().getProductName());
    }

    @Test
    void testSearchOfRecipesByUserProducts() {

        UsersProductEntity usersProduct = new UsersProductEntity();
        usersProduct.setUser(testUser);
        usersProduct.setProduct(testProduct);
        usersProduct.setQuantity(new BigDecimal(2));

        usersProductRepository.save(usersProduct);
        List<RecipeMatchResponse> result = recipeService.searchRecipesByUserProducts(testUser.getId());

        assertEquals(1, result.size());
        assertEquals("Test Recipe", result.getFirst().recipe().getTitle());
    }

    @Test
    void testSearchOfRecipesByUserProductsWithoutMatchingRecipes() {

        List<RecipeMatchResponse> result = recipeService.searchRecipesByUserProducts(testUser.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void testSuccessfulAdditionOfRecipeToFavorites() {

        recipeService.addRecipeToFavorites(testUser.getId(), testRecipe.getId());
        UserEntity user = userRepository.findById(testUser.getId()).orElseThrow();

        assertEquals(1, user.getFavoriteRecipes().size());
        assertEquals("Test Recipe", user.getFavoriteRecipes().iterator().next().getTitle());
    }

    @Test
    void testSuccessfulRemovalOfRecipeFromFavorites() {

        recipeService.addRecipeToFavorites(testUser.getId(), testRecipe.getId());
        recipeService.removeRecipeFromFavorites(testUser.getId(), testRecipe.getId());
        UserEntity user = userRepository.findById(testUser.getId()).orElseThrow();

        assertTrue(user.getFavoriteRecipes().isEmpty());
    }

    @Test
    void testReturnOfUserFavorites() {

        recipeService.addRecipeToFavorites(testUser.getId(), testRecipe.getId());
        List<RecipeDto> result = recipeService.getUserFavorites(testUser.getId());

        assertEquals(1, result.size());
        assertEquals("Test Recipe", result.getFirst().getTitle());
    }

    @Test
    void testReturnOfUserFavoritesWithoutMatchingRecipes() {

        List<RecipeDto> result = recipeService.getUserFavorites(testUser.getId());

        assertTrue(result.isEmpty());
    }
}

