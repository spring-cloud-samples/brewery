package io.spring.cloud.samples.brewery.zookeeper.model;

import com.google.common.collect.Lists;
import lombok.ToString;

import java.util.List;

@ToString
public class Ingredients {

    public Ingredients() {
    }

    public Ingredients(Iterable<Ingredient> ingredients) {
        this.ingredients = Lists.newArrayList(ingredients);
    }

    public List<Ingredient> ingredients = Lists.newArrayList();

}
