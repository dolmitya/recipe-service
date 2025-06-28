package com.recipemaster.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.util.*;

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

    @Column(name = "category")
    private String category;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IngredientEntity> ingredients = new ArrayList<>();

    @ManyToMany(mappedBy = "favoriteRecipes")
    private List<UserEntity> favoritedByUsers = new LinkedList<>();

    public RecipeEntity() {}

    public RecipeEntity(String title, String category, String description) {
        this.title = title;
        this.category = category;
        this.description = description;
    }
}