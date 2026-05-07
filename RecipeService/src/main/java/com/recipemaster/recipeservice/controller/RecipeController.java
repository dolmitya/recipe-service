package com.recipemaster.recipeservice.controller;

import com.recipemaster.dto.RecipeDto;
import com.recipemaster.dto.RecipeInputDto;
import com.recipemaster.dto.responses.RecipeSuggestionDto;
import com.recipemaster.entities.UserEntity;
import com.recipemaster.exceptions.AppError;
import com.recipemaster.recipeservice.service.RecipeService;
import com.recipemaster.recipeservice.service.UserService;
import com.recipemaster.recipeservice.utils.JwtTokenUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequestMapping("/secured/recipes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Рецепты", description = "Каталог рецептов, поиск и избранное")
public class RecipeController {
    private final RecipeService recipeService;
    private final UserService userService;
    private final JwtTokenUtils jwtTokenUtils;

    private UserEntity getUserFromHeader(String authHeader) {
        String jwtToken = authHeader.replace("Bearer ", "");
        String email = jwtTokenUtils.getUsername(jwtToken);
        return userService.findUserEntityByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    @Operation(
            summary = "Получить список рецептов",
            description = "Возвращает все рецепты, а также поддерживает фильтрацию по категории и поиск по названию"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список рецептов",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RecipeDto.class)))
    )
    @GetMapping
    public ResponseEntity<?> getAllRecipes(@RequestParam(required = false) String category,
                                           @RequestParam(required = false) String query) {
        try {
            List<RecipeDto> recipes = recipeService.getAllRecipes(category, query);
            return ResponseEntity.ok(recipes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Ошибка при получении рецептов"));
        }
    }

    @Operation(
            summary = "Создать рецепт",
            description = "Создает рецепт вместе с ингредиентами, количеством порций и расчетной калорийностью"
    )
    @PostMapping
    public ResponseEntity<?> addRecipe(@RequestBody RecipeInputDto recipeInputDto) {
        try {
            RecipeDto recipe = recipeService.addRecipe(recipeInputDto);
            return new ResponseEntity<>(recipe, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AppError(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Ошибка при добавлении рецепта"));
        }
    }

    @Operation(
            summary = "Подобрать рецепты по продуктам",
            description = "Ищет рецепты на основе содержимого холодильника пользователя"
    )
    @GetMapping("/search")
    public ResponseEntity<?> searchRecipes(@RequestHeader("Authorization") String authHeader) {
        try {
            UserEntity user = getUserFromHeader(authHeader);
            List<RecipeDto> recipes = recipeService.searchRecipesByUserProducts(user.getId());
            return ResponseEntity.ok(recipes);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Ошибка при поиске рецептов"));
        }
    }

    @Operation(
            summary = "Подсказки по названиям рецептов",
            description = "Возвращает короткий список подходящих рецептов по мере ввода названия"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Список подсказок по рецептам",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RecipeSuggestionDto.class)))
    )
    @GetMapping("/suggest")
    public ResponseEntity<?> suggestRecipes(@RequestHeader("Authorization") String authHeader,
                                            @RequestParam String query) {
        try {
            getUserFromHeader(authHeader);
            return ResponseEntity.ok(recipeService.suggestRecipes(query));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Ошибка при поиске подсказок по рецептам"));
        }
    }

    @Operation(summary = "Добавить рецепт в избранное")
    @PostMapping("/{recipeId}/favorites")
    public ResponseEntity<?> addFavorite(@PathVariable Long recipeId, @RequestHeader("Authorization") String authHeader) {
        try {
            UserEntity user = getUserFromHeader(authHeader);
            RecipeDto recipe = recipeService.addRecipeToFavorites(user.getId(), recipeId);
            return new ResponseEntity<>(recipe, HttpStatus.CREATED);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Ошибка при добавлении в избранное"));
        }
    }

    @Operation(summary = "Удалить рецепт из избранного")
    @DeleteMapping("/{recipeId}/favorites")
    public ResponseEntity<?> removeFavorite(@PathVariable Long recipeId, @RequestHeader("Authorization") String authHeader) {
        try {
            UserEntity user = getUserFromHeader(authHeader);
            recipeService.removeRecipeFromFavorites(user.getId(), recipeId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Ошибка при удалении из избранного"));
        }
    }

    @Operation(summary = "Получить избранные рецепты пользователя")
    @GetMapping("/favorites")
    public ResponseEntity<?> getFavorites(@RequestHeader("Authorization") String authHeader) {
        try {
            UserEntity user = getUserFromHeader(authHeader);
            List<RecipeDto> favorites = recipeService.getUserFavorites(user.getId());
            return ResponseEntity.ok(favorites);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Ошибка при получении избранных рецептов"));
        }
    }
}
