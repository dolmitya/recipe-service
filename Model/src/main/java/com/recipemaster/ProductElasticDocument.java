package com.recipemaster;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Data
@Document(indexName = "products")
public class ProductElasticDocument {
    @Id
    private String id;
    private String name;
    private String unit;
}