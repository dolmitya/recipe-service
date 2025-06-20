package com.recipemaster;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "products")
@Data
public class ProductDocument {
    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "russian_custom")
    private String name;

    public ProductDocument(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
