package com.recipemaster.recipeservice.integration;

import com.recipemaster.dto.IngredientDto;
import com.recipemaster.dto.RecipeDto;
import com.recipemaster.dto.RecipeInputDto;
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
    void getAllRecipes_WithNoCategory_ReturnsAllRecipes() {

        List<RecipeDto> result = recipeService.getAllRecipes(null);

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Test Recipe", result.getFirst().getTitle());
    }

    @Test
    void getAllRecipes_WithCategory_ReturnsFilteredRecipes() {

        List<RecipeDto> result = recipeService.getAllRecipes("Test Category");

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Test Recipe", result.getFirst().getTitle());
    }

    @Test
    void addRecipe_SuccessfullyAddsRecipe() {

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
        Assertions.assertEquals("New Recipe", result.getTitle());
        Assertions.assertEquals(1, result.getIngredients().size());
        Assertions.assertEquals("Test Product", result.getIngredients().getFirst().getProductName());
    }

    @Test
    void searchRecipesByUserProducts_ReturnsMatchingRecipes() {

        UsersProductEntity usersProduct = new UsersProductEntity();
        usersProduct.setUser(testUser);
        usersProduct.setProduct(testProduct);
        usersProduct.setQuantity(new BigDecimal(2));
        usersProductRepository.save(usersProduct);

        List<RecipeDto> result = recipeService.searchRecipesByUserProducts(testUser.getId());

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Test Recipe", result.getFirst().getTitle());
    }

    @Test
    void searchRecipesByUserProducts_NoMatchingRecipes_ReturnsEmptyList() {

        List<RecipeDto> result = recipeService.searchRecipesByUserProducts(testUser.getId());

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    void addRecipeToFavorites_SuccessfullyAdds() {

        recipeService.addRecipeToFavorites(testUser.getId(), testRecipe.getId());

        UserEntity user = userRepository.findById(testUser.getId()).orElseThrow();
        Assertions.assertEquals(1, user.getFavoriteRecipes().size());
        Assertions.assertEquals("Test Recipe", user.getFavoriteRecipes().iterator().next().getTitle());
    }

    @Test
    void removeRecipeFromFavorites_SuccessfullyRemoves() {

        recipeService.addRecipeToFavorites(testUser.getId(), testRecipe.getId());

        recipeService.removeRecipeFromFavorites(testUser.getId(), testRecipe.getId());

        UserEntity user = userRepository.findById(testUser.getId()).orElseThrow();
        Assertions.assertTrue(user.getFavoriteRecipes().isEmpty());
    }

    @Test
    void getUserFavorites_ReturnsUserFavorites() {

        recipeService.addRecipeToFavorites(testUser.getId(), testRecipe.getId());

        List<RecipeDto> result = recipeService.getUserFavorites(testUser.getId());

        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Test Recipe", result.getFirst().getTitle());
    }

    @Test
    void getUserFavorites_NoFavorites_ReturnsEmptyList() {

        List<RecipeDto> result = recipeService.getUserFavorites(testUser.getId());

        Assertions.assertTrue(result.isEmpty());
    }
}

