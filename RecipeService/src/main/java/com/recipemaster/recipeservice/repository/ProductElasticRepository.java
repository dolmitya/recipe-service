package com.recipemaster.recipeservice.repository;


import com.recipemaster.ProductElasticDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ProductElasticRepository extends ElasticsearchRepository<ProductElasticDocument, String> {
    List<ProductElasticDocument> findByName(String name);
}
