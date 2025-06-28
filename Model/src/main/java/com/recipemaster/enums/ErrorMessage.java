package com.recipemaster.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ErrorMessage {
    USER_NOT_FOUND_BY_ID("Пользователь с id %d не найден"),
    USER_NOT_FOUND_BY_EMAIL("Пользователь с такой почтой не найден"),
    USER_EXISTS("Пользователь с таким именем уже существует"),
    USER_NOT_FOUND_BY_USERNAME("Пользователь с таким именем не найден"),
    USERS_PRODUCT_NOT_FOUND_BY_ID("Данного продукта нет у указанного пользователя"),
    INCORRECT_PRODUCT_UNIT("Единица измерения продукта указана неверно, используйте: "),
    INCORRECT_PRODUCT_NAME("Продукт с таким названием не найден"),
    INCORRECT_USER_DATA("Логин или пароль неверны"),
    INCORRECT_STATUS("Некорректный статус ответа"),
    FORBIDDEN("Доступ запрещен");
    private final String message;

    public String getMessage(Object... args) {
        return String.format(message, args);
    }
}
