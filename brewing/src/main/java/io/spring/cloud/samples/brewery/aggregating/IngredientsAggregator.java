package io.spring.cloud.samples.brewery.aggregating;

import java.util.concurrent.CompletableFuture;

import io.spring.cloud.samples.brewery.common.events.Event;
import io.spring.cloud.samples.brewery.common.events.EventGateway;
import io.spring.cloud.samples.brewery.common.events.EventType;
import io.spring.cloud.samples.brewery.common.model.Ingredient;
import io.spring.cloud.samples.brewery.common.model.Ingredients;
import io.spring.cloud.samples.brewery.common.model.Order;
import org.slf4j.Logger;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class IngredientsAggregator {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(IngredientsAggregator.class);
    private final MaturingServiceUpdater maturingUpdater;
    private final IngredientWarehouse ingredientWarehouse;
    private final IngredientsCollector ingredientsCollector;
    private final EventGateway eventGateway;
    private final BeanFactory beanFactory;

    @Autowired
    IngredientsAggregator(IngredientWarehouse ingredientWarehouse, MaturingServiceUpdater maturingServiceUpdater,
            IngredientsCollector ingredientsCollector,
            EventGateway eventGateway, BeanFactory beanFactory) {
        this.ingredientWarehouse = ingredientWarehouse;
        this.ingredientsCollector = ingredientsCollector;
        this.maturingUpdater = maturingServiceUpdater;
        this.eventGateway = eventGateway;
        this.beanFactory = beanFactory;
    }

    // TODO: Consider simplifying the case by removing the DB (always matches threshold)
    public Ingredients fetchIngredients(Order order, String processId) throws Exception {
        log.info("Fetching ingredients for order [{}] , processId [{}]", order, processId);
        /**
         * [OBSERVABILITY] ParallelStreams won't work out of the box
         * - example of a completable future with our TraceableExecutorService
         * - makes little business sense here but that's just an example
         */
        /*CompletableFuture completableFuture = CompletableFuture.supplyAsync(() -> {
                    ingredientsCollector.collectIngredients(order, processId).stream()
                            .filter(ingredient -> ingredient != null)
                            .forEach((Ingredient ingredient) -> {
                                log.info("Adding an ingredient [{}] for order [{}] , processId [{}]", ingredient);
                                ingredientWarehouse.addIngredient(ingredient);
                            });
                    return null;
                }, new TraceableExecutorService(this.beanFactory, Executors.newFixedThreadPool(5), "fetchIngredients"));*/
        CompletableFuture completableFuture = CompletableFuture.supplyAsync(() -> {
                    ingredientsCollector.collectIngredients(order, processId).stream()
                            .filter(ingredient -> ingredient != null)
                            .forEach((Ingredient ingredient) -> {
                                log.info("Adding an ingredient [{}] for order [{}] , processId [{}]", ingredient);
                                ingredientWarehouse.addIngredient(ingredient);
                            });
                    return null;
                });
        // block to perform the request (as I said the example is stupid)
        completableFuture.get();
        eventGateway.emitEvent(Event.builder().eventType(EventType.INGREDIENTS_ORDERED).processId(processId).build());
        Ingredients ingredients = ingredientWarehouse.getCurrentState();
        return maturingUpdater.updateIfLimitReached(ingredients, processId);
    }

}
