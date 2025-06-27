package com.recipemaster.recipeservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.recipemaster.ProductElasticDocument;
import com.recipemaster.entities.ProductEntity;
import com.recipemaster.recipeservice.repository.ProductElasticRepository;
import com.recipemaster.recipeservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductElasticService {

    private final ProductRepository productRepository;
    private final ElasticsearchClient elasticsearchClient;

    public ProductEntity findOrCreate(String name, String unit) {
        try {
            // Поиск с учетом синонимов и морфологии
            SearchResponse<ProductElasticDocument> response = elasticsearchClient.search(s -> s
                            .index("products")
                            .query(q -> q
                                    .match(m -> m
                                            .field("name")
                                            .query(name)
                                    )
                            ),
                    ProductElasticDocument.class
            );

            if (!response.hits().hits().isEmpty()) {
                ProductElasticDocument match = response.hits().hits().get(0).source();
                return productRepository.findByNameIgnoreCase(match.getName())
                        .orElseGet(() -> {
                            ProductEntity newProduct = new ProductEntity();
                            newProduct.setName(match.getName());
                            newProduct.setUnit(unit);
                            return productRepository.save(newProduct);
                        });
            }

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при поиске в Elasticsearch", e);
        }

        // Создание нового продукта
        ProductEntity product = new ProductEntity();
        product.setName(name);
        product.setUnit(unit);
        ProductEntity saved = productRepository.save(product);

        // Индексация в Elasticsearch
        ProductElasticDocument doc = new ProductElasticDocument();
        doc.setId(saved.getId().toString());
        doc.setName(saved.getName());
        doc.setUnit(saved.getUnit());
        try {
            elasticsearchClient.index(i -> i
                    .index("products")
                    .id(doc.getId())
                    .document(doc)
            );
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при индексации в Elasticsearch", e);
        }

        return saved;
    }
}