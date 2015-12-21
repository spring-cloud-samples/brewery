package io.spring.cloud.samples.brewery.aggregating;

import io.spring.cloud.samples.brewery.aggregating.model.Ingredients;
import io.spring.cloud.samples.brewery.aggregating.model.Order;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class IngredientsAggregator {

    private final IngredientsProperties ingredientsProperties;
    private final MaturingServiceUpdater maturingUpdater;
    private final IngredientWarehouse ingredientWarehouse;
    private final IngredientsCollector ingredientsCollector;

    IngredientsAggregator(IngredientsProperties ingredientsProperties,
                          IngredientWarehouse ingredientWarehouse,
                          MaturingServiceUpdater maturingServiceUpdater,
                          IngredientsCollector ingredientsCollector) {
        this.ingredientWarehouse = ingredientWarehouse;
        this.ingredientsCollector = ingredientsCollector;
        this.maturingUpdater = maturingServiceUpdater;
        this.ingredientsProperties = ingredientsProperties;
    }

    // TODO: Consider simplifying the case by removing the DB (always matches threshold)
    Ingredients fetchIngredients(Order order, String processId) {
        log.info("Fetching ingredients for order [{}] , processId [{}]",order, processId);
        ingredientsCollector.collectIngredients(order, processId).stream()
                .filter(ingredient -> ingredient != null)
                .forEach(ingredientWarehouse::addIngredient);
        Ingredients ingredients = ingredientWarehouse.getCurrentState();
        return maturingUpdater.updateIfLimitReached(ingredients, processId);
    }

}
