package com.recipemaster.recipeservice.controller;

import com.recipemaster.dto.CalendarDayDto;
import com.recipemaster.dto.CalendarEntryRequestDto;
import com.recipemaster.entities.UserEntity;
import com.recipemaster.exceptions.AppError;
import com.recipemaster.recipeservice.service.CalendarService;
import com.recipemaster.recipeservice.service.UserService;
import com.recipemaster.recipeservice.utils.JwtTokenUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/secured/calendar")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Календарь питания", description = "Дневник съеденных продуктов и рецептов")
public class CalendarController {
    private final CalendarService calendarService;
    private final UserService userService;
    private final JwtTokenUtils jwtTokenUtils;

    private UserEntity getUserFromHeader(String authHeader) {
        String jwtToken = authHeader.replace("Bearer ", "");
        String email = jwtTokenUtils.getUsername(jwtToken);
        return userService.findUserEntityByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("Пользователь не найден: " + email));
    }

    @Operation(
            summary = "Получить календарь за день",
            description = "Возвращает список всех записей за выбранный день и общую калорийность"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Календарь за день",
                    content = @Content(schema = @Schema(implementation = CalendarDayDto.class))),
            @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                    content = @Content(schema = @Schema(implementation = AppError.class)))
    })
    @GetMapping
    public ResponseEntity<?> getDay(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            UserEntity user = getUserFromHeader(authHeader);
            CalendarDayDto calendarDay = calendarService.getDay(user.getId(), date);
            return ResponseEntity.ok(calendarDay);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Не удалось загрузить календарь"));
        }
    }

    @Operation(
            summary = "Добавить запись в календарь",
            description = "Добавляет в календарь либо продукт, либо рецепт с указанным количеством"
    )
    @PostMapping("/entries")
    public ResponseEntity<?> addEntry(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CalendarEntryRequestDto requestDto
    ) {
        try {
            UserEntity user = getUserFromHeader(authHeader);
            return new ResponseEntity<>(calendarService.addEntry(user.getId(), requestDto), HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new AppError(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Не удалось сохранить запись"));
        }
    }

    @Operation(summary = "Удалить запись из календаря")
    @DeleteMapping("/entries/{entryId}")
    public ResponseEntity<?> deleteEntry(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long entryId
    ) {
        try {
            UserEntity user = getUserFromHeader(authHeader);
            calendarService.deleteEntry(user.getId(), entryId);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AppError(HttpStatus.NOT_FOUND.value(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new AppError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Не удалось удалить запись"));
        }
    }
}
