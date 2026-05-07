package com.recipemaster.recipeservice.service;

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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CalendarService {
    private static final String PRODUCT_ENTRY = "PRODUCT";
    private static final String RECIPE_ENTRY = "RECIPE";
    private static final String RECIPE_UNIT = "порц.";

    private final MealEntryRepository mealEntryRepository;
    private final UserRepository userRepository;
    private final UsersProductRepository usersProductRepository;
    private final RecipeRepository recipeRepository;

    public CalendarDayDto getDay(Long userId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<CalendarEntryDto> entries = mealEntryRepository.findAllByUserIdAndConsumedOnOrderByIdDesc(userId, targetDate)
                .stream()
                .map(this::toCalendarEntryDto)
                .toList();

        BigDecimal totalCalories = entries.stream()
                .map(CalendarEntryDto::getCalories)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalProteins = entries.stream()
                .map(CalendarEntryDto::getProteins)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalFats = entries.stream()
                .map(CalendarEntryDto::getFats)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCarbs = entries.stream()
                .map(CalendarEntryDto::getCarbs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CalendarDayDto(targetDate, totalCalories, totalProteins, totalFats, totalCarbs, entries);
    }

    @Transactional
    public CalendarEntryDto addEntry(Long userId, CalendarEntryRequestDto requestDto) {
        validateRequest(requestDto);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        MealEntryEntity mealEntry = new MealEntryEntity();
        mealEntry.setUser(user);
        mealEntry.setConsumedOn(Optional.ofNullable(requestDto.getDate()).orElse(LocalDate.now()));
        mealEntry.setQuantity(requestDto.getQuantity());

        if (requestDto.getProductId() != null) {
            UsersProductEntity usersProduct = usersProductRepository.findProductById(userId, requestDto.getProductId())
                    .orElseThrow(() -> new NoSuchElementException("Product not found in user's fridge"));
            if (shouldConsumeFromFridge(requestDto)) {
                consumeProduct(usersProduct, requestDto.getQuantity());
            }
            mealEntry.setProduct(usersProduct.getProduct());
        } else {
            RecipeEntity recipe = recipeRepository.findById(requestDto.getRecipeId())
                    .orElseThrow(() -> new NoSuchElementException("Recipe not found"));
            if (shouldConsumeFromFridge(requestDto)) {
                consumeRecipeIngredients(userId, recipe, requestDto.getQuantity());
            }
            mealEntry.setRecipe(recipe);
        }

        MealEntryEntity savedEntry = mealEntryRepository.save(mealEntry);
        return toCalendarEntryDto(savedEntry);
    }

    public void deleteEntry(Long userId, Long entryId) {
        MealEntryEntity mealEntry = mealEntryRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new NoSuchElementException("Calendar entry not found"));
        mealEntryRepository.delete(mealEntry);
    }

    private void validateRequest(CalendarEntryRequestDto requestDto) {
        if (requestDto == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (requestDto.getQuantity() == null || requestDto.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        boolean hasProduct = requestDto.getProductId() != null;
        boolean hasRecipe = requestDto.getRecipeId() != null;
        if (hasProduct == hasRecipe) {
            throw new IllegalArgumentException("Specify exactly one source: productId or recipeId");
        }
    }

    private boolean shouldConsumeFromFridge(CalendarEntryRequestDto requestDto) {
        return !Boolean.FALSE.equals(requestDto.getConsumeFromFridge());
    }

    private void consumeProduct(UsersProductEntity usersProduct, BigDecimal consumedQuantity) {
        if (usersProduct.getQuantity().compareTo(consumedQuantity) < 0) {
            throw new IllegalArgumentException(
                    "Невозможно списать продукт. Недостаточно: "
                            + usersProduct.getProduct().getName()
                            + ", нужно " + consumedQuantity.stripTrailingZeros().toPlainString() + normalizedUnit(usersProduct.getProduct().getUnit())
                            + ", есть " + usersProduct.getQuantity().stripTrailingZeros().toPlainString() + normalizedUnit(usersProduct.getProduct().getUnit())
            );
        }

        BigDecimal remainingQuantity = usersProduct.getQuantity().subtract(consumedQuantity);
        if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            usersProductRepository.deleteByUserAndProductId(usersProduct.getUser().getId(), usersProduct.getProduct().getId());
        } else {
            usersProduct.setQuantity(remainingQuantity);
            usersProductRepository.save(usersProduct);
        }
    }

    private void consumeRecipeIngredients(Long userId, RecipeEntity recipe, BigDecimal eatenServings) {
        BigDecimal totalServings = Optional.ofNullable(recipe.getServings())
                .filter(value -> value.compareTo(BigDecimal.ZERO) > 0)
                .orElse(BigDecimal.ONE);
        BigDecimal multiplier = eatenServings.divide(totalServings, 6, RoundingMode.HALF_UP);
        StringBuilder shortage = new StringBuilder();

        for (IngredientEntity ingredient : recipe.getIngredients()) {
            BigDecimal requiredQuantity = ingredient.getQuantity().multiply(multiplier);
            UsersProductEntity usersProduct = usersProductRepository.findProductById(userId, ingredient.getProduct().getId())
                    .orElse(null);
            BigDecimal availableQuantity = usersProduct != null ? usersProduct.getQuantity() : BigDecimal.ZERO;

            if (availableQuantity.compareTo(requiredQuantity) < 0) {
                if (!shortage.isEmpty()) {
                    shortage.append("; ");
                }
                shortage.append(buildShortageMessage(ingredient, requiredQuantity, availableQuantity));
            }
        }

        if (!shortage.isEmpty()) {
            throw new IllegalArgumentException("Невозможно приготовить рецепт. Недостаточно продуктов: " + shortage);
        }

        for (IngredientEntity ingredient : recipe.getIngredients()) {
            BigDecimal requiredQuantity = ingredient.getQuantity().multiply(multiplier);
            UsersProductEntity usersProduct = usersProductRepository.findProductById(userId, ingredient.getProduct().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Продукт отсутствует в холодильнике: " + ingredient.getProduct().getName()));
            BigDecimal remainingQuantity = usersProduct.getQuantity().subtract(requiredQuantity);

            if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                usersProductRepository.deleteByUserAndProductId(userId, ingredient.getProduct().getId());
            } else {
                usersProduct.setQuantity(remainingQuantity);
                usersProductRepository.save(usersProduct);
            }
        }
    }

    private String buildShortageMessage(IngredientEntity ingredient,
                                        BigDecimal requiredQuantity,
                                        BigDecimal availableQuantity) {
        String unit = normalizedUnit(ingredient.getProduct().getUnit());
        return ingredient.getProduct().getName()
                + ": нужно " + requiredQuantity.stripTrailingZeros().toPlainString() + unit
                + ", есть " + availableQuantity.stripTrailingZeros().toPlainString() + unit;
    }

    private String normalizedUnit(String unit) {
        return unit != null && !unit.isBlank() ? " " + unit : "";
    }

    private CalendarEntryDto toCalendarEntryDto(MealEntryEntity mealEntry) {
        if (mealEntry.getProduct() != null) {
            ProductEntity product = mealEntry.getProduct();
            BigDecimal quantity = mealEntry.getQuantity();
            BigDecimal calories = normalizedValue(product.getCaloriesPerUnit()).multiply(quantity);
            BigDecimal proteins = normalizedValue(product.getProteinsPerUnit()).multiply(quantity);
            BigDecimal fats = normalizedValue(product.getFatsPerUnit()).multiply(quantity);
            BigDecimal carbs = normalizedValue(product.getCarbsPerUnit()).multiply(quantity);
            return new CalendarEntryDto(
                    mealEntry.getId(),
                    PRODUCT_ENTRY,
                    product.getId(),
                    product.getName(),
                    quantity,
                    product.getUnit(),
                    calories,
                    proteins,
                    fats,
                    carbs
            );
        }

        RecipeEntity recipe = mealEntry.getRecipe();
        BigDecimal servings = Optional.ofNullable(recipe.getServings())
                .filter(value -> value.compareTo(BigDecimal.ZERO) > 0)
                .orElse(BigDecimal.ONE);
        BigDecimal totalRecipeCalories = recipe.getIngredients().stream()
                .map(ingredient -> normalizedValue(ingredient.getProduct().getCaloriesPerUnit()).multiply(ingredient.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRecipeProteins = recipe.getIngredients().stream()
                .map(ingredient -> normalizedValue(ingredient.getProduct().getProteinsPerUnit()).multiply(ingredient.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRecipeFats = recipe.getIngredients().stream()
                .map(ingredient -> normalizedValue(ingredient.getProduct().getFatsPerUnit()).multiply(ingredient.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRecipeCarbs = recipe.getIngredients().stream()
                .map(ingredient -> normalizedValue(ingredient.getProduct().getCarbsPerUnit()).multiply(ingredient.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal caloriesPerServing = totalRecipeCalories.divide(servings, 2, RoundingMode.HALF_UP);
        BigDecimal proteinsPerServing = totalRecipeProteins.divide(servings, 2, RoundingMode.HALF_UP);
        BigDecimal fatsPerServing = totalRecipeFats.divide(servings, 2, RoundingMode.HALF_UP);
        BigDecimal carbsPerServing = totalRecipeCarbs.divide(servings, 2, RoundingMode.HALF_UP);
        BigDecimal quantity = mealEntry.getQuantity();

        return new CalendarEntryDto(
                mealEntry.getId(),
                RECIPE_ENTRY,
                recipe.getId(),
                recipe.getTitle(),
                quantity,
                RECIPE_UNIT,
                caloriesPerServing.multiply(quantity),
                proteinsPerServing.multiply(quantity),
                fatsPerServing.multiply(quantity),
                carbsPerServing.multiply(quantity)
        );
    }

    private BigDecimal normalizedValue(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
