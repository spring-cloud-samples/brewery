package io.spring.cloud.samples.brewery.eureka.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Order {
    private List<IngredientType> items = new ArrayList<>();
}
