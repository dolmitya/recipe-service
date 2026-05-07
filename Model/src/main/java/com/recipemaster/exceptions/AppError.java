package com.recipemaster.exceptions;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "Структура ошибки API")
public class AppError {
    @Schema(description = "HTTP статус", example = "400")
    private int status;
    @Schema(description = "Текст ошибки", example = "Некорректный запрос")
    private String message;
    @Schema(description = "Время формирования ошибки")
    private Date timestamp;

    public AppError(int status, String message) {
        this.status = status;
        this.message = message;
        this.timestamp = new Date();
    }
}
