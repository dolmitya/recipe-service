package com.recipemaster.recipeservice.repository;

import com.recipemaster.entities.UsersProductEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsersProductRepository extends CrudRepository<UsersProductEntity, Long> {
    List<UsersProductEntity> findAllByUserId(Long userId);

    @Query("SELECT up FROM UsersProductEntity up WHERE up.user.id = :userId AND up.product.id = :productId")
    Optional<UsersProductEntity> findProductById(Long userId, Long productId);

    @Modifying
    @Query("DELETE FROM UsersProductEntity up WHERE up.user.id = :userId AND up.product.id = :productId")
    void deleteByUserAndProductId(Long userId, Long productId);
}
