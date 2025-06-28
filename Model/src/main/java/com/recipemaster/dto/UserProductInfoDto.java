package com.recipemaster.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class UserProductInfoDto {
    private Long id;
    private String name;
    private BigDecimal quantity;
    private String unit;
}
