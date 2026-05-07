package com.recipemaster.dto.responses;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT токен для авторизованных запросов")
public record JwtResponse(String token) {
}
