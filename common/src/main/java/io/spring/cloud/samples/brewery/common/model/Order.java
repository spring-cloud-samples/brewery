package io.spring.cloud.samples.brewery.common.model;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Order {
    private List<IngredientType> items = new ArrayList<>();
}
