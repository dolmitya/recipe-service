package com.recipemaster.recipeservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.recipemaster.ProductElasticDocument;
import com.recipemaster.entities.ProductEntity;
import com.recipemaster.recipeservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReindexService {

    private final ProductRepository repository;
    private final ElasticsearchClient client;

    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void reindexAll() {
        log.info("Reindex started");

        try {
            Iterable<ProductEntity> products = repository.findAll();
            products.forEach(this::index);
        } catch (Exception e) {
            log.error("Reindex failed", e);
        }

        log.info("Reindex finished");
    }

    private void index(ProductEntity product) {
        try {
            client.index(i -> i
                    .index("products")
                    .id(product.getId().toString())
                    .document(toDoc(product))
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ProductElasticDocument toDoc(ProductEntity product) {
        ProductElasticDocument doc = new ProductElasticDocument();
        doc.setId(product.getId().toString());
        doc.setName(product.getName());
        doc.setUnit(product.getUnit());
        return doc;
    }
}
