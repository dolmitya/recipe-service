package com.recipemaster.recipeservice.controller;

import com.recipemaster.dto.UserProductInfoDto;
import com.recipemaster.entities.UserEntity;
import com.recipemaster.exceptions.AppError;
import com.recipemaster.recipeservice.service.UserService;
import com.recipemaster.recipeservice.service.UsersProductService;
import com.recipemaster.recipeservice.utils.JwtTokenUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
import org.springframework.web.bind.annotation.RestController;


import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/secured/products")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ProductController {
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
            summary = "Получить список продуктов пользователя",
            description = "Возвращает список всех продуктов, привязанных к пользователю: название, количество и единица измерения"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Список успешно получен"),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден", content = @io.swagger.v3.oas.annotations.media.Content(schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = AppError.class)))
    })
    @GetMapping()
    public ResponseEntity<?> getUserProducts(
            @Parameter(description = "JWT токен авторизации", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
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

    @PostMapping
    @Operation(summary = "Добавить новый продукт в холодильник")
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

    @PutMapping("/{productId}")
    @Operation(summary = "Обновить продукт в холодильнике")
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
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Ошибка при обновлении продукта"));
        }
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Удалить продукт из холодильника")
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
}
