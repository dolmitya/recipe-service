package com.recipemaster.entities;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Entity
@Table(name = "recipe")
@Data
public class RecipeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private RecipeCategoryEntity recipeCategory;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal servings = BigDecimal.ONE;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IngredientEntity> ingredients = new ArrayList<>();

    @ManyToMany(mappedBy = "favoriteRecipes")
    private List<UserEntity> favoritedByUsers = new LinkedList<>();

    public RecipeEntity() {}

    public RecipeEntity(String title, String category, String description) {
        this.title = title;
        setCategory(category);
        this.description = description;
    }

    public String getCategory() {
        return recipeCategory == null ? null : recipeCategory.getName();
    }

    public void setCategory(String category) {
        this.recipeCategory = category == null || category.isBlank()
                ? null
                : new RecipeCategoryEntity(category);
    }
}
