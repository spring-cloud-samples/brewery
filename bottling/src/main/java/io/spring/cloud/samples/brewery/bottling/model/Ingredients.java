package io.spring.cloud.samples.brewery.bottling.model;

import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;

public class Ingredients implements Iterable<Ingredient> {

    public Ingredients() {
    }

    public Ingredients(Iterable<Ingredient> ingredients) {
        this.ingredients = Lists.newArrayList(ingredients);
    }

    public List<Ingredient> ingredients = Lists.newArrayList();

    @Override
    public Iterator<Ingredient> iterator() {
        return ingredients.iterator();
    }
}
