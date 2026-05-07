package com.recipemaster;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Data
@Document(indexName = "recipes")
public class RecipeElasticDocument {
    @Id
    private String id;
    private String title;
    private String category;
}
