package com.recipemaster.dto.requests;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record RegistrationRequest(
        @NotNull
        @JsonProperty("email") String email,

        @NotNull
        @JsonProperty("password") String password,

        @NotNull
        @JsonProperty("fullName") String fullName
) {
}