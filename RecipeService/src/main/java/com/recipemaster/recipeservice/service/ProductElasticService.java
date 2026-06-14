package com.recipemaster.recipeservice.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.recipemaster.ProductElasticDocument;
import com.recipemaster.entities.ProductEntity;
import com.recipemaster.recipeservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.recipemaster.recipeservice.utils.ProductNameNormalizer.normalizeDisplayName;

@Service
@RequiredArgsConstructor
public class ProductElasticService {

    private final ProductRepository productRepository;
    private final ElasticsearchClient elasticsearchClient;

    public ProductEntity findOrCreate(String name, String unit) {
        return findOrCreate(name, unit, null, null, null, null);
    }

    public ProductEntity findOrCreate(String name, String unit, BigDecimal caloriesPerUnit) {
        return findOrCreate(name, unit, caloriesPerUnit, null, null, null);
    }

    public ProductEntity findOrCreate(String name,
                                      String unit,
                                      BigDecimal caloriesPerUnit,
                                      BigDecimal proteinsPerUnit,
                                      BigDecimal fatsPerUnit,
                                      BigDecimal carbsPerUnit) {
        String normalizedName = normalizeName(name);
        String normalizedUnit = normalizeUnit(unit);

        ProductEntity fromDb = findBestMatchingProduct(normalizedName, normalizedUnit);
        if (fromDb != null) {
            return enrichProduct(fromDb, normalizedUnit, caloriesPerUnit, proteinsPerUnit, fatsPerUnit, carbsPerUnit);
        }

        ProductEntity saved = createProductInDb(
                normalizedName,
                normalizedUnit,
                caloriesPerUnit,
                proteinsPerUnit,
                fatsPerUnit,
                carbsPerUnit
        );
        indexInElastic(saved);
        return saved;
    }

    private ProductEntity createProductInDb(String name,
                                            String unit,
                                            BigDecimal caloriesPerUnit,
                                            BigDecimal proteinsPerUnit,
                                            BigDecimal fatsPerUnit,
                                            BigDecimal carbsPerUnit) {
        ProductEntity existingProduct = findBestMatchingProduct(name, unit);
        if (existingProduct != null) {
            return enrichProduct(existingProduct, unit, caloriesPerUnit, proteinsPerUnit, fatsPerUnit, carbsPerUnit);
        }

        ProductEntity product = new ProductEntity();
        product.setName(name);
        product.setUnit(unit);
        applyNutrition(product, caloriesPerUnit, proteinsPerUnit, fatsPerUnit, carbsPerUnit);
        return productRepository.save(product);
    }

    private ProductEntity enrichProduct(ProductEntity product,
                                        String unit,
                                        BigDecimal caloriesPerUnit,
                                        BigDecimal proteinsPerUnit,
                                        BigDecimal fatsPerUnit,
                                        BigDecimal carbsPerUnit) {
        boolean changed = false;

        if (product.getUnit() == null && unit != null && !unit.isBlank()) {
            product.setUnit(unit);
            changed = true;
        } else if (product.getUnit() != null && unit != null && !unit.isBlank() && !product.getUnit().equals(unit)) {
            throw new IllegalArgumentException("Unit for this product does not match the existing value");
        }

        changed |= mergeNutritionValue(
                product.getCaloriesPerUnit(),
                caloriesPerUnit,
                product::setCaloriesPerUnit,
                "Calories for this product do not match the existing value"
        );
        changed |= mergeNutritionValue(
                product.getProteinsPerUnit(),
                proteinsPerUnit,
                product::setProteinsPerUnit,
                "Proteins for this product do not match the existing value"
        );
        changed |= mergeNutritionValue(
                product.getFatsPerUnit(),
                fatsPerUnit,
                product::setFatsPerUnit,
                "Fats for this product do not match the existing value"
        );
        changed |= mergeNutritionValue(
                product.getCarbsPerUnit(),
                carbsPerUnit,
                product::setCarbsPerUnit,
                "Carbs for this product do not match the existing value"
        );

        if (!changed) {
            return product;
        }

        ProductEntity saved = productRepository.save(product);
        indexInElastic(saved);
        return saved;
    }

    private boolean mergeNutritionValue(BigDecimal existingValue,
                                        BigDecimal incomingValue,
                                        java.util.function.Consumer<BigDecimal> setter,
                                        String mismatchMessage) {
        BigDecimal normalizedExisting = normalizeNutritionValue(existingValue, "Existing nutrition value cannot be negative");
        BigDecimal normalizedIncoming = normalizeNutritionValue(incomingValue, mismatchMessage);

        if (normalizedExisting.compareTo(BigDecimal.ZERO) == 0 && normalizedIncoming.compareTo(BigDecimal.ZERO) > 0) {
            setter.accept(normalizedIncoming);
            return true;
        }

        if (normalizedExisting.compareTo(BigDecimal.ZERO) > 0
                && normalizedIncoming.compareTo(BigDecimal.ZERO) > 0
                && normalizedExisting.compareTo(normalizedIncoming) != 0) {
            throw new IllegalArgumentException(mismatchMessage);
        }

        return false;
    }

    private void applyNutrition(ProductEntity product,
                                BigDecimal caloriesPerUnit,
                                BigDecimal proteinsPerUnit,
                                BigDecimal fatsPerUnit,
                                BigDecimal carbsPerUnit) {
        product.setCaloriesPerUnit(normalizeNutritionValue(caloriesPerUnit, "Calories cannot be negative"));
        product.setProteinsPerUnit(normalizeNutritionValue(proteinsPerUnit, "Proteins cannot be negative"));
        product.setFatsPerUnit(normalizeNutritionValue(fatsPerUnit, "Fats cannot be negative"));
        product.setCarbsPerUnit(normalizeNutritionValue(carbsPerUnit, "Carbs cannot be negative"));
    }

    private BigDecimal normalizeNutritionValue(BigDecimal value, String negativeMessage) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(negativeMessage);
        }
        return value;
    }

    private ProductEntity findBestMatchingProduct(String name, String unit) {
        return productRepository.findAllByNameIgnoreCase(name).stream()
                .filter(product -> unit == null || unit.equals(product.getUnit()))
                .max(Comparator
                        .comparing((ProductEntity product) -> Optional.ofNullable(product.getCaloriesPerUnit()).orElse(BigDecimal.ZERO))
                        .thenComparing(ProductEntity::getId))
                .orElse(null);
    }

    private void indexInElastic(ProductEntity product) {
        ProductElasticDocument doc = buildDocument(product);
        try {
            elasticsearchClient.index(i -> i
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

    public List<ProductEntity> suggestProducts(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return List.of();
        }

        String normalizedPrefix = prefix.trim();

        try {
            SearchResponse<ProductElasticDocument> response = elasticsearchClient.search(s -> s
                            .index("products")
                            .size(20)
                            .query(q -> q
                                    .matchPhrasePrefix(m -> m
                                            .field("name")
                                            .query(normalizedPrefix)
                                    )
                            ),
                    ProductElasticDocument.class
            );

            List<Long> ids = response.hits().hits().stream()
                    .map(hit -> hit.source())
                    .filter(Objects::nonNull)
                    .map(ProductElasticDocument::getId)
                    .map(Long::valueOf)
                    .toList();

            Map<Long, ProductEntity> productsById = new LinkedHashMap<>();
            productRepository.findAllById(ids).forEach(product -> productsById.put(product.getId(), product));

            Map<String, ProductEntity> deduplicated = new LinkedHashMap<>();
            for (Long id : ids) {
                ProductEntity product = productsById.get(id);
                if (product == null) {
                    continue;
                }
                String key = buildSuggestionKey(product);
                ProductEntity existing = deduplicated.get(key);
                if (existing == null || compareSuggestionPriority(product, existing) > 0) {
                    deduplicated.put(key, product);
                }
            }

            List<ProductEntity> suggestions = deduplicated.values().stream()
                    .limit(10)
                    .collect(Collectors.toList());
            if (!suggestions.isEmpty()) {
                return suggestions;
            }
        } catch (IOException e) {
            return suggestProductsFromDatabase(normalizedPrefix);
        }

        return suggestProductsFromDatabase(normalizedPrefix);
    }

    private int compareSuggestionPriority(ProductEntity left, ProductEntity right) {
        return Comparator
                .comparing((ProductEntity product) -> Optional.ofNullable(product.getCaloriesPerUnit()).orElse(BigDecimal.ZERO))
                .thenComparing(ProductEntity::getId)
                .compare(left, right);
    }

    private String buildSuggestionKey(ProductEntity product) {
        return product.getName().toLowerCase(Locale.ROOT) + "|" + Optional.ofNullable(product.getUnit()).orElse("");
    }

    private List<ProductEntity> suggestProductsFromDatabase(String prefix) {
        Map<String, ProductEntity> deduplicated = new LinkedHashMap<>();
        for (ProductEntity product : productRepository.findTop20ByNameStartingWithIgnoreCaseOrderByNameAscIdAsc(prefix)) {
            String key = buildSuggestionKey(product);
            ProductEntity existing = deduplicated.get(key);
            if (existing == null || compareSuggestionPriority(product, existing) > 0) {
                deduplicated.put(key, product);
            }
        }
        return deduplicated.values().stream()
                .limit(10)
                .collect(Collectors.toList());
    }

    private String normalizeName(String name) {
        return normalizeDisplayName(name);
    }

    private String normalizeUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return null;
        }
        return unit.trim();
    }
}
