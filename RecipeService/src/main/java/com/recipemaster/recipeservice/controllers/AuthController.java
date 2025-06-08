package com.recipemaster.recipeservice.controllers;

import com.recipemaster.recipeservice.services.UserService;
import com.recipemaster.recipeservice.utils.JwtTokenUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final JwtTokenUtils jwtTokenUtils;
    private final AuthenticationManager authenticationManager;

    @Operation(
            summary = "Аутентификация пользователя",
            description = "Возвращает JWT токен для авторизованных запросов"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешная аутентификация",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(
                    responseCode = "401",
                    description = "Неверные учетные данные",
                    content = @Content(schema = @Schema(implementation = AppError.class)))
    })
    @PostMapping("/auth")
    public ResponseEntity<?> createAuthToken(@RequestBody JwtRequest jwtRequest) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(jwtRequest.username(),
                    jwtRequest.password()));
        } catch (BadCredentialsException e) {
            return new ResponseEntity<>(new AppError(HttpStatus.UNAUTHORIZED.value(),
                    ErrorMessage.INCORRECT_USER_DATA.getMessage()), HttpStatus.UNAUTHORIZED);
        }

        UserDetails userDetails = userService.loadUserByUsername(jwtRequest.username());
        String token = jwtTokenUtils.generateToken(userDetails);

        return ResponseEntity.ok(new JwtResponse(token));

    }

    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Создает нового пользователя и возвращает JWT токен"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешная регистрация",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class)))
            ,
            @ApiResponse(
                    responseCode = "400",
                    description = "Пользователь уже существует или невалидные данные",
                    content = @Content(schema = @Schema(implementation = AppError.class)))
    })
    @PostMapping("/signup")
    public ResponseEntity<?> registration(@Validated @RequestBody RegistrationRequest registrationRequest) {

        if (userService.findUserEntityByUsername(registrationRequest.username()).isPresent()) {
            return new ResponseEntity<>(new AppError(HttpStatus.BAD_REQUEST.value(), ErrorMessage.USER_EXISTS.getMessage()), HttpStatus.BAD_REQUEST);
        }

        UserDto userDTO = new UserDto(registrationRequest.username(), registrationRequest.password());
        userService.createNewUser(userDTO);

        return createAuthToken(new JwtRequest(registrationRequest.username(), registrationRequest.password()));
    }

    @Operation(
            summary = "Проверка токена",
            description = "Проверяет валидность JWT токена"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Токен валиден",
            content = @Content(schema = @Schema(implementation = Boolean.class)))
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/secured/checkToken")
    public boolean checkToken() {
        return true;
    }

    @Operation(
            summary = "Получение текущего пользователя",
            description = "Возвращает информацию о текущем авторизованном пользователе"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Информация о пользователе",
                    content = @Content(schema = @Schema(implementation = GetUserResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(schema = @Schema(implementation = AppError.class))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("secured/user")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String jwtToken = authHeader.replace("Bearer ", "");
        String username = jwtTokenUtils.getUsername(jwtToken);

        return userService.findUserEntityByUsername(username)
                .map(user -> {
                    GetUserResponse response = new GetUserResponse(user.getId(), user.getUsername());
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
