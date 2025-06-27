package com.recipemaster.entities;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "users_product")
@Data
@NoArgsConstructor
public class UsersProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;

    public UsersProductEntity(String name, String quantity, String unit) {
        ProductEntity product = new ProductEntity();
        product.setName(name);
        product.setUnit(unit);

        UsersProductEntity usersProduct = new UsersProductEntity();
        usersProduct.setProduct(product);
        usersProduct.setQuantity(new BigDecimal(quantity));
    }
}
