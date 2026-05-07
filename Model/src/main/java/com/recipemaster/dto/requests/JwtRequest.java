package com.recipemaster.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Данные для входа пользователя")
public record JwtRequest(String email, String password) {
}
