package com.recipemaster.recipeservice.utils;

import com.recipemaster.dto.IngredientDto;
import com.recipemaster.dto.RecipeInputDto;
import com.recipemaster.recipeservice.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class RecipeGenerator {

    private final RecipeService recipeService;
    private final Random rnd = new Random();

    private static final List<String> SAMPLE_PRODUCTS = List.of(
            "молоко", "яйца", "мука", "сахар", "сливочное масло", "соль", "помидор", "сыр", "лук", "чеснок",
            "куркума","банан","курица","свинина","бекон","огурец"
    );
    private static final List<String> SAMPLE_UNITS = List.of(
            "шт", "г", "кг", "мл", "л", "ст.л.", "ч.л."
    );
    private static final List<String> SAMPLE_CATEGORIES = List.of(
            "Завтрак", "Обед", "Ужин", "Десерт", "Перекус"
    );
    private static final List<String> SAMPLE_DESCRIPTIONS = List.of(
            "Вкусное и простое блюдо", "Семейный любимый рецепт", "Быстро и легко приготовить", "Традиционное блюдо", "Полезный выбор"
    );

    public void generateAndAdd(int count) {
        IntStream.range(0, count)
                .mapToObj(i -> createRandomRecipeInput())
                .forEach(recipeService::addRecipe);
    }

    private RecipeInputDto createRandomRecipeInput() {
        RecipeInputDto dto = new RecipeInputDto();
        dto.setTitle("Рецепт " + UUID.randomUUID().toString().substring(0, 8));
        dto.setDescription(SAMPLE_DESCRIPTIONS.get(rnd.nextInt(SAMPLE_DESCRIPTIONS.size())));
        dto.setCategory(SAMPLE_CATEGORIES.get(rnd.nextInt(SAMPLE_CATEGORIES.size())));

        int ingredientCount = 1 + rnd.nextInt(4);
        List<IngredientDto> ingredients = IntStream.range(0, ingredientCount)
                .mapToObj(i -> createRandomIngredient())
                .toList();
        dto.setIngredients(ingredients);

        return dto;
    }

    private IngredientDto createRandomIngredient() {
        String product = SAMPLE_PRODUCTS.get(rnd.nextInt(SAMPLE_PRODUCTS.size()));
        String unit = SAMPLE_UNITS.get(rnd.nextInt(SAMPLE_UNITS.size()));
        BigDecimal qty = BigDecimal.valueOf(1 + rnd.nextDouble() * 9)
                .setScale(2, BigDecimal.ROUND_HALF_UP);

        IngredientDto ing = new IngredientDto();
        ing.setProductName(product);
        ing.setQuantity(qty);
        ing.setUnit(unit);
        return ing;
    }
}