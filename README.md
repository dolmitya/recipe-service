# 🧊 SmartFridge — персональный помощник на кухне

**SmartFridge** — это веб-приложение на базе Java + Spring Boot, которое позволяет пользователю управлять "умным холодильником": отслеживать продукты, находить рецепты из доступных ингредиентов, добавлять свои рецепты и делиться ими с другими. Под капотом — продвинутая система поиска с поддержкой русской морфологии и синонимов (например, "огурчик", "корнишон" → "огурец").

## 🚀 Функциональные возможности

- 🔐 **JWT-аутентификация и авторизация**
  - Безопасный вход/регистрация пользователей с использованием Spring Security и JWT.
  
- 🥒 **Умный холодильник**
  - Добавляй продукты, указывай их количество и дату добавления.
  - Автоматическое приведение названий к единому виду (например, "огурчик", "корнишон" → "огурец").

- 🍳 **Рекомендации рецептов**
  - Поиск наиболее подходящих рецептов на основе текущих продуктов.
  - Возможность добавлять свои рецепты.

- ❤️ **Лайки на рецепты**
  - Голосование за любимые рецепты.
  
- 🔍 **Поиск по рецептам с морфологией**
  - Интеграция с **Elasticsearch** с русской морфологией и словарем синонимов.
  - Поиск по названиям и ингредиентам работает "по-человечески".

## 🛠️ Технологии

| Технология | Назначение |
|------------|------------|
| **Java 17+** | Язык реализации |
| **Spring Boot** | Бэкенд фреймворк |
| **Spring Security + JWT** | Аутентификация и авторизация |
| **Spring Data JPA** | Работа с базой данных |
| **PostgreSQL** | Основная база данных |
| **Flyway** | Управление миграциями БД |
| **Elasticsearch** | Поиск с морфологией и синонимами |
| **Swagger/OpenAPI** | Документация API |
| **Gradle** | Система сборки проекта |
