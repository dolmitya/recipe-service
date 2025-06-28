package com.recipemaster.recipeservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.recipemaster.ProductElasticDocument;
import com.recipemaster.entities.ProductEntity;
import com.recipemaster.recipeservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class ProductElasticService {

    private final ProductRepository productRepository;
    private final ElasticsearchClient elasticsearchClient;

    public ProductEntity findOrCreate(String name, String unit) {
        ProductEntity fromEs = searchInElastic(name, unit);
        if (fromEs != null) {
            return fromEs;
        }

        ProductEntity saved = createProductInDb(name, unit);
        indexInElastic(saved);
        return saved;
    }

    private ProductEntity searchInElastic(String name, String unit) {
        try {
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
                ProductElasticDocument hit = response.hits().hits().get(0).source();
                return productRepository
                        .findByNameIgnoreCase(hit.getName())
                        .orElseGet(() -> {
                            ProductEntity e = new ProductEntity();
                            e.setName(hit.getName());
                            e.setUnit(unit);
                            return productRepository.save(e);
                        });
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при поиске в Elasticsearch", e);
        }
        return null;
    }

    private ProductEntity createProductInDb(String name, String unit) {
        ProductEntity product = new ProductEntity();
        product.setName(name);
        product.setUnit(unit);
        return productRepository.save(product);
    }

    private void indexInElastic(ProductEntity product) {
        ProductElasticDocument doc = buildDocument(product);
        try {
            IndexResponse resp = elasticsearchClient.index(i -> i
                    .index("products")
                    .id(doc.getId())
                    .document(doc)
            );
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при индексации в Elasticsearch", e);
        }
    }

    private ProductElasticDocument buildDocument(ProductEntity product) {
        ProductElasticDocument doc = new ProductElasticDocument();
        doc.setId(product.getId().toString());
        doc.setName(product.getName());
        doc.setUnit(product.getUnit());
        return doc;
    }
}
