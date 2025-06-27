package com.recipemaster.recipeservice.controller;

import com.recipemaster.dto.RecipeDto;
import com.recipemaster.dto.RecipeInputDto;
import com.recipemaster.dto.responses.RecipeMatchResponse;
import com.recipemaster.entities.UserEntity;
import com.recipemaster.exceptions.AppError;
import com.recipemaster.recipeservice.service.RecipeService;
import com.recipemaster.recipeservice.service.UserService;
import com.recipemaster.recipeservice.utils.JwtTokenUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/secured/recipes")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
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

    @GetMapping
    @Operation(summary = "Получить список рецептов")
    public ResponseEntity<?> getAllRecipes(@RequestParam(required = false) String category) {
        try {
            List<RecipeDto> recipes = recipeService.getAllRecipes(category);
            return ResponseEntity.ok(recipes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Ошибка при получении рецептов"));
        }
    }

    @PostMapping
    @Operation(summary = "Добавить новый рецепт")
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

    @GetMapping("/search")
    @Operation(summary = "Найти рецепты по продуктам в холодильнике")
    public ResponseEntity<?> searchRecipes(@RequestHeader("Authorization") String authHeader) {
        try {
            UserEntity user = getUserFromHeader(authHeader);
            List<RecipeMatchResponse> recipes = recipeService.searchRecipesByUserProducts(user.getId());
            return ResponseEntity.ok(recipes);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Ошибка при поиске рецептов"));
        }
    }

    @PostMapping("/{recipeId}/favorites")
    @Operation(summary = "Добавить рецепт в избранное")
    public ResponseEntity<?> addFavorite(@PathVariable Long recipeId, @RequestHeader("Authorization") String authHeader) {
        try {
            UserEntity user = getUserFromHeader(authHeader);
            recipeService.addRecipeToFavorites(user.getId(), recipeId);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Ошибка при добавлении в избранное"));
        }
    }

    @DeleteMapping("/{recipeId}/favorites")
    @Operation(summary = "Удалить рецепт из избранного")
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

    @GetMapping("/favorites")
    @Operation(summary = "Получить список избранных рецептов пользователя")
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
