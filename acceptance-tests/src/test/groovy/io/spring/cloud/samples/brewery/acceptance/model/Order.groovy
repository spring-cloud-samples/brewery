package io.spring.cloud.samples.brewery.acceptance.model

import groovy.transform.Canonical

@Canonical
class Order {
    List<IngredientType> items = []
}
