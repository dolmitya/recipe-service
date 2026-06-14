package com.recipemaster.recipeservice.unit;

import com.recipemaster.dto.CalendarDayDto;
import com.recipemaster.dto.CalendarEntryDto;
import com.recipemaster.dto.CalendarEntryRequestDto;
import com.recipemaster.entities.IngredientEntity;
import com.recipemaster.entities.MealEntryEntity;
import com.recipemaster.entities.ProductEntity;
import com.recipemaster.entities.RecipeEntity;
import com.recipemaster.entities.UserEntity;
import com.recipemaster.entities.UsersProductEntity;
import com.recipemaster.recipeservice.repository.MealEntryRepository;
import com.recipemaster.recipeservice.repository.RecipeRepository;
import com.recipemaster.recipeservice.repository.UserRepository;
import com.recipemaster.recipeservice.repository.UsersProductRepository;
import com.recipemaster.recipeservice.service.CalendarService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    private MealEntryRepository mealEntryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UsersProductRepository usersProductRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @InjectMocks
    private CalendarService calendarService;

    private UserEntity user;
    private ProductEntity chicken;
    private ProductEntity rice;
    private UsersProductEntity userChicken;
    private RecipeEntity recipe;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(1L);
        user.setEmail("demo@recipe-service.local");

        chicken = product(10L, "chicken", "g", "1.65", "0.31", "0.04", "0.00");
        rice = product(11L, "rice", "g", "3.30", "0.07", "0.01", "0.74");

        userChicken = new UsersProductEntity();
        userChicken.setUser(user);
        userChicken.setProduct(chicken);
        userChicken.setQuantity(new BigDecimal("500"));

        recipe = recipe(20L, "Chicken rice", new BigDecimal("2"));
        recipe.setIngredients(List.of(
                ingredient(recipe, chicken, new BigDecimal("200")),
                ingredient(recipe, rice, new BigDecimal("100"))
        ));
    }

    @Test
    void getDay_WhenEntriesExist() {
        LocalDate date = LocalDate.of(2026, 5, 9);
        MealEntryEntity productEntry = productEntry(1L, user, chicken, date, new BigDecimal("100"));
        MealEntryEntity recipeEntry = recipeEntry(2L, user, recipe, date, new BigDecimal("1"));
        when(mealEntryRepository.findAllByUserIdAndConsumedOnOrderByIdDesc(user.getId(), date))
                .thenReturn(List.of(productEntry, recipeEntry));

        CalendarDayDto result = calendarService.getDay(user.getId(), date);

        assertEquals(date, result.getDate());
        assertEquals(2, result.getEntries().size());
        assertEquals(new BigDecimal("495.00"), result.getTotalCalories());
        assertEquals(new BigDecimal("65.50"), result.getTotalProteins());
        assertEquals(new BigDecimal("8.50"), result.getTotalFats());
        assertEquals(new BigDecimal("37.00"), result.getTotalCarbs());
    }

    @Test
    void getDay_WhenDateIsNull() {
        when(mealEntryRepository.findAllByUserIdAndConsumedOnOrderByIdDesc(org.mockito.ArgumentMatchers.eq(user.getId()), any(LocalDate.class)))
                .thenReturn(List.of());

        CalendarDayDto result = calendarService.getDay(user.getId(), null);

        assertTrue(result.getEntries().isEmpty());
        assertEquals(BigDecimal.ZERO, result.getTotalCalories());
        verify(mealEntryRepository).findAllByUserIdAndConsumedOnOrderByIdDesc(org.mockito.ArgumentMatchers.eq(user.getId()), any(LocalDate.class));
    }

    @Test
    void addEntry_WhenProductIsConsumedAndStockIsReduced() {
        CalendarEntryRequestDto request = productRequest(chicken.getId(), new BigDecimal("150"), true);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(usersProductRepository.findProductById(user.getId(), chicken.getId())).thenReturn(Optional.of(userChicken));
        when(mealEntryRepository.save(any(MealEntryEntity.class))).thenAnswer(invocation -> {
            MealEntryEntity saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        CalendarEntryDto result = calendarService.addEntry(user.getId(), request);

        assertEquals("PRODUCT", result.getEntryType());
        assertEquals(new BigDecimal("247.50"), result.getCalories());
        assertEquals(new BigDecimal("46.50"), result.getProteins());
        assertEquals(new BigDecimal("350"), userChicken.getQuantity());
        verify(usersProductRepository).save(userChicken);
        verify(mealEntryRepository).save(any(MealEntryEntity.class));
    }

    @Test
    void addEntry_WhenProductIsConsumedWithoutStockReduction() {
        CalendarEntryRequestDto request = productRequest(chicken.getId(), new BigDecimal("150"), false);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(usersProductRepository.findProductById(user.getId(), chicken.getId())).thenReturn(Optional.of(userChicken));
        when(mealEntryRepository.save(any(MealEntryEntity.class))).thenAnswer(invocation -> {
            MealEntryEntity saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        CalendarEntryDto result = calendarService.addEntry(user.getId(), request);

        assertEquals(new BigDecimal("500"), userChicken.getQuantity());
        assertEquals(new BigDecimal("247.50"), result.getCalories());
        verify(usersProductRepository, never()).save(userChicken);
        verify(usersProductRepository, never()).deleteByUserAndProductId(any(), any());
    }

    @Test
    void addEntry_WhenConsumedProductQuantityExceedsStock() {
        CalendarEntryRequestDto request = productRequest(chicken.getId(), new BigDecimal("600"), true);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(usersProductRepository.findProductById(user.getId(), chicken.getId())).thenReturn(Optional.of(userChicken));

        assertThrows(IllegalArgumentException.class, () -> calendarService.addEntry(user.getId(), request));
        verify(mealEntryRepository, never()).save(any());
    }

    @Test
    void addEntry_WhenProductIsNotFoundInFridge() {
        CalendarEntryRequestDto request = productRequest(chicken.getId(), new BigDecimal("100"), true);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(usersProductRepository.findProductById(user.getId(), chicken.getId())).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> calendarService.addEntry(user.getId(), request));

        assertEquals("Product not found in user's fridge", exception.getMessage());
        verify(mealEntryRepository, never()).save(any());
    }

    @Test
    void addEntry_WhenRecipeIsConsumedWithoutStockReduction() {
        CalendarEntryRequestDto request = recipeRequest(recipe.getId(), new BigDecimal("1"), false);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(recipeRepository.findById(recipe.getId())).thenReturn(Optional.of(recipe));
        when(mealEntryRepository.save(any(MealEntryEntity.class))).thenAnswer(invocation -> {
            MealEntryEntity saved = invocation.getArgument(0);
            saved.setId(101L);
            return saved;
        });

        CalendarEntryDto result = calendarService.addEntry(user.getId(), request);

        assertEquals("RECIPE", result.getEntryType());
        assertEquals(new BigDecimal("330.00"), result.getCalories());
        assertEquals(new BigDecimal("34.50"), result.getProteins());
        verify(usersProductRepository, never()).findProductById(any(), any());
        verify(mealEntryRepository).save(any(MealEntryEntity.class));
    }

    @Test
    void addEntry_WhenRecipeIsConsumedAndIngredientsAreReduced() {
        CalendarEntryRequestDto request = recipeRequest(recipe.getId(), new BigDecimal("1"), true);
        UsersProductEntity fridgeChicken = usersProduct(user, chicken, new BigDecimal("300"));
        UsersProductEntity fridgeRice = usersProduct(user, rice, new BigDecimal("100"));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(recipeRepository.findById(recipe.getId())).thenReturn(Optional.of(recipe));
        when(usersProductRepository.findProductById(user.getId(), chicken.getId())).thenReturn(Optional.of(fridgeChicken));
        when(usersProductRepository.findProductById(user.getId(), rice.getId())).thenReturn(Optional.of(fridgeRice));
        when(mealEntryRepository.save(any(MealEntryEntity.class))).thenAnswer(invocation -> {
            MealEntryEntity saved = invocation.getArgument(0);
            saved.setId(101L);
            return saved;
        });

        CalendarEntryDto result = calendarService.addEntry(user.getId(), request);

        assertEquals(new BigDecimal("330.00"), result.getCalories());
        assertEquals(new BigDecimal("200.000000"), fridgeChicken.getQuantity());
        assertEquals(new BigDecimal("50.000000"), fridgeRice.getQuantity());
        verify(usersProductRepository).save(fridgeChicken);
        verify(usersProductRepository).save(fridgeRice);
    }

    @Test
    void addEntry_WhenRecipeIngredientsAreMissing() {
        CalendarEntryRequestDto request = recipeRequest(recipe.getId(), new BigDecimal("1"), true);
        UsersProductEntity fridgeChicken = usersProduct(user, chicken, new BigDecimal("20"));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(recipeRepository.findById(recipe.getId())).thenReturn(Optional.of(recipe));
        when(usersProductRepository.findProductById(user.getId(), chicken.getId())).thenReturn(Optional.of(fridgeChicken));
        when(usersProductRepository.findProductById(user.getId(), rice.getId())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> calendarService.addEntry(user.getId(), request));
        verify(mealEntryRepository, never()).save(any());
    }

    @Test
    void addEntry_WhenRequestIsNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> calendarService.addEntry(user.getId(), null));

        assertEquals("Request body is required", exception.getMessage());
    }

    @Test
    void addEntry_WhenQuantityIsNotPositive() {
        CalendarEntryRequestDto request = productRequest(chicken.getId(), BigDecimal.ZERO, true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> calendarService.addEntry(user.getId(), request));

        assertEquals("Quantity must be greater than zero", exception.getMessage());
    }

    @Test
    void addEntry_WhenBothSourcesAreProvided() {
        CalendarEntryRequestDto request = productRequest(chicken.getId(), new BigDecimal("100"), true);
        request.setRecipeId(recipe.getId());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> calendarService.addEntry(user.getId(), request));

        assertEquals("Specify exactly one source: productId or recipeId", exception.getMessage());
    }

    @Test
    void deleteEntry_WhenEntryExists() {
        MealEntryEntity entry = productEntry(1L, user, chicken, LocalDate.of(2026, 5, 9), new BigDecimal("100"));
        when(mealEntryRepository.findByIdAndUserId(entry.getId(), user.getId())).thenReturn(Optional.of(entry));

        calendarService.deleteEntry(user.getId(), entry.getId());

        verify(mealEntryRepository).delete(entry);
    }

    @Test
    void deleteEntry_WhenEntryNotFound() {
        when(mealEntryRepository.findByIdAndUserId(404L, user.getId())).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(NoSuchElementException.class, () -> calendarService.deleteEntry(user.getId(), 404L));

        assertEquals("Calendar entry not found", exception.getMessage());
        verify(mealEntryRepository, never()).delete(any());
    }

    private CalendarEntryRequestDto productRequest(Long productId, BigDecimal quantity, Boolean consumeFromFridge) {
        CalendarEntryRequestDto request = new CalendarEntryRequestDto();
        request.setDate(LocalDate.of(2026, 5, 9));
        request.setProductId(productId);
        request.setQuantity(quantity);
        request.setConsumeFromFridge(consumeFromFridge);
        return request;
    }

    private CalendarEntryRequestDto recipeRequest(Long recipeId, BigDecimal quantity, Boolean consumeFromFridge) {
        CalendarEntryRequestDto request = new CalendarEntryRequestDto();
        request.setDate(LocalDate.of(2026, 5, 9));
        request.setRecipeId(recipeId);
        request.setQuantity(quantity);
        request.setConsumeFromFridge(consumeFromFridge);
        return request;
    }

    private ProductEntity product(Long id, String name, String unit, String calories, String proteins, String fats, String carbs) {
        ProductEntity product = new ProductEntity();
        product.setId(id);
        product.setName(name);
        product.setUnit(unit);
        product.setCaloriesPerUnit(new BigDecimal(calories));
        product.setProteinsPerUnit(new BigDecimal(proteins));
        product.setFatsPerUnit(new BigDecimal(fats));
        product.setCarbsPerUnit(new BigDecimal(carbs));
        return product;
    }

    private RecipeEntity recipe(Long id, String title, BigDecimal servings) {
        RecipeEntity recipe = new RecipeEntity();
        recipe.setId(id);
        recipe.setTitle(title);
        recipe.setServings(servings);
        return recipe;
    }

    private IngredientEntity ingredient(RecipeEntity recipe, ProductEntity product, BigDecimal quantity) {
        IngredientEntity ingredient = new IngredientEntity();
        ingredient.setRecipe(recipe);
        ingredient.setProduct(product);
        ingredient.setQuantity(quantity);
        return ingredient;
    }

    private UsersProductEntity usersProduct(UserEntity user, ProductEntity product, BigDecimal quantity) {
        UsersProductEntity usersProduct = new UsersProductEntity();
        usersProduct.setUser(user);
        usersProduct.setProduct(product);
        usersProduct.setQuantity(quantity);
        return usersProduct;
    }

    private MealEntryEntity productEntry(Long id, UserEntity user, ProductEntity product, LocalDate date, BigDecimal quantity) {
        MealEntryEntity entry = new MealEntryEntity();
        entry.setId(id);
        entry.setUser(user);
        entry.setProduct(product);
        entry.setConsumedOn(date);
        entry.setQuantity(quantity);
        return entry;
    }

    private MealEntryEntity recipeEntry(Long id, UserEntity user, RecipeEntity recipe, LocalDate date, BigDecimal quantity) {
        MealEntryEntity entry = new MealEntryEntity();
        entry.setId(id);
        entry.setUser(user);
        entry.setRecipe(recipe);
        entry.setConsumedOn(date);
        entry.setQuantity(quantity);
        return entry;
    }
}
