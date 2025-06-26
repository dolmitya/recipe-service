package com.recipemaster.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class UserProductInfoDto {
    private String productName;
    private BigDecimal quantity;
    private String unit;
}
