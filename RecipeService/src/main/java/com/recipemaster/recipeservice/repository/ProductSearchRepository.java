package com.recipemaster.recipeservice.repository;

import com.recipemaster.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {
    List<ProductDocument> findByName(String name); // можно расширить для full-text поиска
}
