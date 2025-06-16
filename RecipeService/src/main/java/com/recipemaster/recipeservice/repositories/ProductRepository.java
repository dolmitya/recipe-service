package com.recipemaster.recipeservice.repositories;

import com.recipemaster.entities.ProductEntity;
import org.springframework.data.repository.CrudRepository;

public interface ProductRepository extends CrudRepository<ProductEntity, Long> {
}
