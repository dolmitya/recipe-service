package com.recipemaster.recipeservice.repositories;

import com.recipemaster.entities.UsersProductEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Repository
public interface UsersProductRepository extends CrudRepository<UsersProductEntity, Long> {
    @Query("SELECT up FROM UsersProductEntity up WHERE up.user.id = :userId")
    List<UsersProductEntity> findAllByUserId(Long userId);

    @Query("SELECT up FROM UsersProductEntity up WHERE up.user.id = :userId AND up.product.id = :productId")
    UsersProductEntity findProductById(Long userId, Long productId);

    @Query("UPDATE UsersProductEntity up SET up.quantity = :quantity WHERE up.user.id = :userId AND up.product.id = :productId")
    List<UsersProductEntity> updateProductQuantity(Long userId, Long productId, BigDecimal quantity);
}
