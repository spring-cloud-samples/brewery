package io.spring.cloud.samples.brewery.maturing.model;

import com.google.common.collect.Lists;

import java.util.List;

public class Ingredients {

    public Ingredients() {
    }

    public Ingredients(Iterable<Ingredient> ingredients) {
        this.ingredients = Lists.newArrayList(ingredients);
    }

    public List<Ingredient> ingredients = Lists.newArrayList();

}
