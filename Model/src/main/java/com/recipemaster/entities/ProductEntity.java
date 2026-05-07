package com.recipemaster.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product")
@Data
@NoArgsConstructor
public class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String unit;

    @Column(name = "calories_per_unit", nullable = false, precision = 10, scale = 2)
    private BigDecimal caloriesPerUnit = BigDecimal.ZERO;

    @Column(name = "proteins_per_unit", nullable = false, precision = 10, scale = 2)
    private BigDecimal proteinsPerUnit = BigDecimal.ZERO;

    @Column(name = "fats_per_unit", nullable = false, precision = 10, scale = 2)
    private BigDecimal fatsPerUnit = BigDecimal.ZERO;

    @Column(name = "carbs_per_unit", nullable = false, precision = 10, scale = 2)
    private BigDecimal carbsPerUnit = BigDecimal.ZERO;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IngredientEntity> ingredients = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UsersProductEntity> usersProducts = new ArrayList<>();
}
