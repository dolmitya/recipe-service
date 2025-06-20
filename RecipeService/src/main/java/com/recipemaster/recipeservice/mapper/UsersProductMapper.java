package com.recipemaster.recipeservice.mapper;

import com.recipemaster.dto.UserProductInfoDto;
import com.recipemaster.entities.ProductEntity;
import com.recipemaster.entities.UserEntity;
import com.recipemaster.entities.UsersProductEntity;

public class UsersProductMapper {
    public static UsersProductEntity toUsersProductEntity(
            UserEntity user,
            ProductEntity product,
            UserProductInfoDto productInputDto) {
        UsersProductEntity usersProduct = new UsersProductEntity();

        usersProduct.setUser(user);
        usersProduct.setProduct(product);
        usersProduct.setQuantity(productInputDto.getQuantity());
        return usersProduct;
    }
}
