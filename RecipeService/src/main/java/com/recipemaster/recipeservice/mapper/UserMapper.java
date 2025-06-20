package com.recipemaster.recipeservice.mapper;

import com.recipemaster.dto.UserDetailsDto;
import com.recipemaster.dto.UserDto;
import com.recipemaster.entities.UserEntity;

public final class UserMapper {

    public static UserEntity UserDTOToUserEntity(UserDto userDTO) {
        return new UserEntity(userDTO.email(), userDTO.password(), userDTO.fullName());
    }

    public static UserDetailsDto toUserDetailsDto(UserEntity user) {
        return new UserDetailsDto(
                user.getId(),
                user.getEmail()
        );
    }
}