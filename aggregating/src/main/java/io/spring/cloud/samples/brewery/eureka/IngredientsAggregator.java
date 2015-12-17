package io.spring.cloud.samples.brewery.eureka;

import static com.netflix.hystrix.HystrixCommand.Setter.withGroupKey;
import static com.netflix.hystrix.HystrixCommandGroupKey.Factory.asKey;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import io.spring.cloud.samples.brewery.eureka.model.IngredientType;
import io.spring.cloud.samples.brewery.eureka.model.Ingredients;
import org.springframework.cloud.sleuth.TraceManager;
import org.springframework.cloud.sleuth.instrument.hystrix.TraceCommand;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.HystrixCommandKey;
import lombok.extern.slf4j.Slf4j;
import io.spring.cloud.samples.brewery.eureka.model.Ingredient;
import io.spring.cloud.samples.brewery.eureka.model.Order;

@Slf4j
class IngredientsAggregator {

    private static final Integer DEFAULT_QUANTITY = 1000;

    private final IngredientsProperties ingredientsProperties;
    private final MaturingServiceUpdater maturingUpdater;
    private final IngredientWarehouse ingredientWarehouse;
    private final TraceManager traceManager;

    IngredientsAggregator(IngredientsProperties ingredientsProperties,
                          IngredientWarehouse ingredientWarehouse,
                          TraceManager traceManager,
                          MaturingServiceClient maturingServiceClient, RestTemplate restTemplate) {
        this.ingredientWarehouse = ingredientWarehouse;
        this.traceManager = traceManager;
        this.maturingUpdater = new MaturingServiceUpdater(ingredientsProperties,
                ingredientWarehouse, maturingServiceClient, restTemplate);
        this.ingredientsProperties = ingredientsProperties;
    }

    // TODO: Consider simplifying the case by removing the DB (always matches threshold)
    Ingredients fetchIngredients(Order order, String processId) {
        ingredients(order).stream()
                .filter(ingredient -> ingredient != null)
                .forEach(ingredientWarehouse::addIngredient);
        Ingredients ingredients = ingredientWarehouse.getCurrentState();
        return maturingUpdater.updateIfLimitReached(ingredients, processId);
    }

    private List<Ingredient> ingredients(Order order) {
        return order.getItems()
                .stream()
                .map(ingredientType -> new Ingredient(ingredientType, DEFAULT_QUANTITY))
                .collect(Collectors.toList());
    }

}
