package com.recipemaster.recipeservice.controller;

import com.recipemaster.dto.UserDto;
import com.recipemaster.dto.requests.JwtRequest;
import com.recipemaster.dto.requests.RegistrationRequest;
import com.recipemaster.dto.responses.JwtResponse;
import com.recipemaster.enums.ErrorMessage;
import com.recipemaster.exceptions.AppError;
import com.recipemaster.recipeservice.service.UserService;
import com.recipemaster.recipeservice.utils.JwtTokenUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Авторизация", description = "Регистрация и вход пользователей")
public class AuthController {
    private final UserService userService;
    private final JwtTokenUtils jwtTokenUtils;
    private final AuthenticationManager authenticationManager;

    @Operation(
            summary = "Вход пользователя",
            description = "Проверяет email и пароль и возвращает JWT токен"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешный вход",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Неверные учетные данные",
                    content = @Content(schema = @Schema(implementation = AppError.class))
            )
    })
    @PostMapping("/login")
    public ResponseEntity<?> createAuthToken(@RequestBody JwtRequest jwtRequest) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(jwtRequest.email(),
                    jwtRequest.password()));
        } catch (BadCredentialsException e) {
            return new ResponseEntity<>(new AppError(HttpStatus.UNAUTHORIZED.value(),
                    ErrorMessage.INCORRECT_USER_DATA.getMessage()), HttpStatus.UNAUTHORIZED);
        }

        UserDetails userDetails = userService.loadUserByUsername(jwtRequest.email());
        String token = jwtTokenUtils.generateToken(userDetails);

        return ResponseEntity.ok(new JwtResponse(token));
    }

    @Operation(
            summary = "Регистрация пользователя",
            description = "Создает нового пользователя и сразу возвращает JWT токен"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователь успешно зарегистрирован",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Пользователь уже существует или тело запроса невалидно",
                    content = @Content(schema = @Schema(implementation = AppError.class))
            )
    })
    @PostMapping("/register")
    public ResponseEntity<?> registration(@Validated @RequestBody RegistrationRequest registrationRequest) {
        if (userService.findUserEntityByEmail(registrationRequest.email()).isPresent()) {
            return new ResponseEntity<>(new AppError(HttpStatus.BAD_REQUEST.value(), ErrorMessage.USER_EXISTS.getMessage()),
                    HttpStatus.BAD_REQUEST);
        }

        UserDto userDTO = new UserDto(registrationRequest.email(), registrationRequest.password(), registrationRequest.fullName());
        userService.createNewUser(userDTO);

        return createAuthToken(new JwtRequest(registrationRequest.email(), registrationRequest.password()));
    }
}
