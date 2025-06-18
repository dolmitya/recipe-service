package com.recipemaster.recipeservice.repositories;

import com.recipemaster.entities.ProductEntity;
import com.recipemaster.entities.UsersProductEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ProductRepository extends CrudRepository<ProductEntity, Long> {
    @Query("SELECT p FROM ProductEntity p WHERE p.name = :productName")
    ProductEntity findByName(String productName);
}
