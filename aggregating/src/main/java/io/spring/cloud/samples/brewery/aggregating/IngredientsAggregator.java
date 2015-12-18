package io.spring.cloud.samples.brewery.aggregating;

import java.util.List;
import java.util.stream.Collectors;

import io.spring.cloud.samples.brewery.aggregating.model.Ingredients;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;
import io.spring.cloud.samples.brewery.aggregating.model.Ingredient;
import io.spring.cloud.samples.brewery.aggregating.model.Order;

@Slf4j
class IngredientsAggregator {

    private final IngredientsProperties ingredientsProperties;
    private final MaturingServiceUpdater maturingUpdater;
    private final IngredientWarehouse ingredientWarehouse;
    private final Integer threshold;

    IngredientsAggregator(IngredientsProperties ingredientsProperties,
                          IngredientWarehouse ingredientWarehouse,
                          MaturingServiceClient maturingServiceClient, RestTemplate restTemplate) {
        this.ingredientWarehouse = ingredientWarehouse;
        this.threshold = ingredientsProperties.getThreshold();
        this.maturingUpdater = new MaturingServiceUpdater(ingredientsProperties,
                ingredientWarehouse, maturingServiceClient, restTemplate);
        this.ingredientsProperties = ingredientsProperties;
    }

    // TODO: Consider simplifying the case by removing the DB (always matches threshold)
    Ingredients fetchIngredients(Order order, String processId) {
        log.info("Fetching ingredients for order [{}] , processId [{}] and threshold [{}]",
                order, processId, threshold);
        ingredients(order).stream()
                .filter(ingredient -> ingredient != null)
                .forEach(ingredientWarehouse::addIngredient);
        Ingredients ingredients = ingredientWarehouse.getCurrentState();
        return maturingUpdater.updateIfLimitReached(ingredients, processId);
    }

    private List<Ingredient> ingredients(Order order) {
        return order.getItems()
                .stream()
                .map(ingredientType -> new Ingredient(ingredientType, ingredientsProperties.getReturnedIngredientsQuantity()))
                .collect(Collectors.toList());
    }

}
