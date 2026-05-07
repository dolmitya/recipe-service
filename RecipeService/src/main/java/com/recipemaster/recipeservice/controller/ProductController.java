package com.recipemaster.recipeservice.controller;

import com.recipemaster.dto.UserProductInfoDto;
import com.recipemaster.dto.responses.ProductSuggestionDto;
import com.recipemaster.entities.UserEntity;
import com.recipemaster.exceptions.AppError;
import com.recipemaster.recipeservice.service.ProductElasticService;
import com.recipemaster.recipeservice.service.UserService;
import com.recipemaster.recipeservice.service.UsersProductService;
import com.recipemaster.recipeservice.utils.JwtTokenUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/secured/products")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Продукты", description = "Работа с холодильником пользователя и подсказками продуктов")
public class ProductController {
    private final ProductElasticService productElasticService;
    private final UsersProductService usersProductService;
    private final UserService userService;
    private final JwtTokenUtils jwtTokenUtils;

    private UserEntity getUserFromHeader(String authHeader) {
        String jwtToken = authHeader.replace("Bearer ", "");
        String email = jwtTokenUtils.getUsername(jwtToken);
        return userService.findUserEntityByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("Пользователь с почтой " + email + " не найден"));
    }

    @Operation(
            summary = "Получить продукты пользователя",
            description = "Возвращает содержимое холодильника пользователя вместе с количеством, калориями и БЖУ"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Список продуктов",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserProductInfoDto.class)))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(schema = @Schema(implementation = AppError.class))
            )
    })
    @GetMapping
    public ResponseEntity<?> getUserProducts(
            @Parameter(description = "JWT токен авторизации", required = true)
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            UserEntity user = getUserFromHeader(authHeader);
            List<UserProductInfoDto> userProducts = usersProductService.getUserProductsByUserId(user.getId());
            return ResponseEntity.ok(userProducts);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        }
    }

    @Operation(
            summary = "Добавить продукт в холодильник",
            description = "Создает новый продукт или увеличивает количество уже существующего. Калории и БЖУ задаются один раз при создании"
    )
    @PostMapping
    public ResponseEntity<?> addProduct(@RequestHeader("Authorization") String authHeader,
                                        @RequestBody UserProductInfoDto productInputDto) {
        try {
            UserEntity user = getUserFromHeader(authHeader);
            UserProductInfoDto product = usersProductService.addProduct(user.getId(), productInputDto);
            return new ResponseEntity<>(product, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AppError(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Ошибка при добавлении продукта"));
        }
    }

    @Operation(
            summary = "Обновить продукт в холодильнике",
            description = "Изменяет только количество продукта в холодильнике"
    )
    @PutMapping("/{productId}")
    public ResponseEntity<?> updateProduct(@RequestHeader("Authorization") String authHeader,
                                           @PathVariable Long productId,
                                           @RequestBody UserProductInfoDto productInputDto) {
        try {
            UserEntity user = getUserFromHeader(authHeader);
            UserProductInfoDto updatedProduct = usersProductService.updateProduct(user.getId(), productId, productInputDto);
            return ResponseEntity.ok(updatedProduct);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), "Продукт не найден"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AppError(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Ошибка при обновлении продукта"));
        }
    }

    @Operation(summary = "Удалить продукт из холодильника")
    @DeleteMapping("/{productId}")
    public ResponseEntity<?> deleteProduct(@RequestHeader("Authorization") String authHeader,
                                           @PathVariable Long productId) {
        try {
            UserEntity user = getUserFromHeader(authHeader);
            usersProductService.deleteProduct(user.getId(), productId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), "Продукт не найден"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Ошибка при удалении продукта"));
        }
    }

    @Operation(
            summary = "Подсказки продуктов",
            description = "Возвращает список подсказок по введенному префиксу названия продукта вместе с калориями и БЖУ"
    )
    @GetMapping("/suggest")
    public ResponseEntity<?> suggestProducts(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String query
    ) {
        try {
            getUserFromHeader(authHeader);

            List<ProductSuggestionDto> suggestions = productElasticService.suggestProducts(query)
                    .stream()
                    .map(product -> new ProductSuggestionDto(
                            product.getName(),
                            product.getUnit(),
                            product.getCaloriesPerUnit(),
                            product.getProteinsPerUnit(),
                            product.getFatsPerUnit(),
                            product.getCarbsPerUnit()
                    ))
                    .toList();

            return ResponseEntity.ok(suggestions);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Ошибка при поиске подсказок"));
        }
    }
}
