package com.recipemaster.recipeservice.repository;

import com.recipemaster.entities.ProductEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface ProductRepository extends CrudRepository<ProductEntity, Long> {
    ProductEntity findByName(String productName);
    Optional<ProductEntity> findByNameIgnoreCase(String name);
}
