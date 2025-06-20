package com.recipemaster.recipeservice.service;

import com.recipemaster.ProductDocument;
import com.recipemaster.entities.ProductEntity;
import com.recipemaster.recipeservice.repository.ProductRepository;
import com.recipemaster.recipeservice.repository.ProductSearchRepository;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductSearchRepository productSearchRepository;

    public ProductService(ProductRepository productRepository,
                          ProductSearchRepository productSearchRepository) {
        this.productRepository = productRepository;
        this.productSearchRepository = productSearchRepository;
    }

    @Transactional
    public ProductEntity findOrCreateByName(String inputName) {
        // Поиск в Elasticsearch с применением русского анализатора
        List<ProductDocument> found = productSearchRepository.search(
                QueryBuilders.matchQuery("name", inputName)
                        .analyzer("russian_custom")
        );

        if (!found.isEmpty()) {
            // Если нашли — берём первый, возвращаем из базы по id
            String foundId = found.get(0).getId();
            return productRepository.findById(Long.parseLong(foundId))
                    .orElseGet(() -> createNewProduct(inputName));
        }

        // Если в Elasticsearch не нашли — создаём новый продукт
        return createNewProduct(inputName);
    }

    private ProductEntity createNewProduct(String name) {
        ProductEntity product = new ProductEntity();
        product.setName(name);
        product = productRepository.save(product);

        // Сохраняем в Elasticsearch, id преобразуем в строку
        ProductDocument doc = new ProductDocument(product.getId().toString(), product.getName());
        productSearchRepository.save(doc);

        return product;
    }
}
