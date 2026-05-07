package com.recipemaster.dto.requests;


import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Данные для регистрации пользователя")
public record RegistrationRequest(
        @Schema(description = "Email пользователя", example = "demo@example.com")
        @NotNull
        @JsonProperty("email") String email,

        @Schema(description = "Пароль пользователя", example = "Password123!")
        @NotNull
        @JsonProperty("password") String password,

        @Schema(description = "Отображаемое имя пользователя", example = "Тестовый пользователь")
        @NotNull
        @JsonProperty("fullName") String fullName
) {
}
