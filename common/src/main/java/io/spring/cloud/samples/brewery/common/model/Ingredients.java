package io.spring.cloud.samples.brewery.common.model;

import java.util.List;

import com.google.common.collect.Lists;

public class Ingredients {

    public Ingredients() {
    }

    public Ingredients(Iterable<Ingredient> ingredients) {
        this.ingredients = Lists.newArrayList(ingredients);
    }

    public List<Ingredient> ingredients = Lists.newArrayList();

    public String toString() {
        return "Ingredients(ingredients=" + this.ingredients + ")";
    }
}
