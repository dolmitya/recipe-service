package com.recipemaster.recipeservice.repository;

import com.recipemaster.entities.ProductEntity;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends CrudRepository<ProductEntity, Long> {
    Optional<ProductEntity> findByName(String productName);
    Optional<ProductEntity> findByNameIgnoreCase(String productName);
    List<ProductEntity> findAllByNameIgnoreCase(String productName);
    List<ProductEntity> findTop20ByNameStartingWithIgnoreCaseOrderByNameAscIdAsc(String prefix);
}
