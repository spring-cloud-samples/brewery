package io.spring.cloud.samples.brewery.common.model;

import java.util.ArrayList;
import java.util.List;

public class Ingredients {

    public List<Ingredient> ingredients = new ArrayList<>();

    public Ingredients() {
    }

    public Ingredients(Iterable<Ingredient> ingredients) {
        this.ingredients = new ArrayList<>();
        ingredients.iterator().forEachRemaining(this.ingredients::add);
    }

    public String toString() {
        return "Ingredients(ingredients=" + this.ingredients + ")";
    }
}
