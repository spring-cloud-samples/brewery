package io.spring.cloud.samples.brewery.common.model;

import lombok.Data;

@Data
public class Ingredient {
    private IngredientType type;
    private Integer quantity;

    public Ingredient(IngredientType type, Integer quantity) {
        this.type = type;
        this.quantity = quantity;
    }

    public Ingredient() {
    }
}
