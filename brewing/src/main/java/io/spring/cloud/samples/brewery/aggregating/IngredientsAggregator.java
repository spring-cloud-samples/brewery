package io.spring.cloud.samples.brewery.aggregating;

import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import io.spring.cloud.samples.brewery.common.events.Event;
import io.spring.cloud.samples.brewery.common.events.EventGateway;
import io.spring.cloud.samples.brewery.common.events.EventType;
import io.spring.cloud.samples.brewery.common.model.Ingredient;
import io.spring.cloud.samples.brewery.common.model.Ingredients;
import io.spring.cloud.samples.brewery.common.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.SpanName;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.async.TraceableExecutorService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Slf4j
class IngredientsAggregator {

    private final MaturingServiceUpdater maturingUpdater;
    private final IngredientWarehouse ingredientWarehouse;
    private final IngredientsCollector ingredientsCollector;
    private final Tracer tracer;
    private final EventGateway eventGateway;

    IngredientsAggregator(IngredientWarehouse ingredientWarehouse,
                          MaturingServiceUpdater maturingServiceUpdater,
                          IngredientsCollector ingredientsCollector,
                          Tracer tracer, EventGateway eventGateway) {
        this.ingredientWarehouse = ingredientWarehouse;
        this.ingredientsCollector = ingredientsCollector;
        this.maturingUpdater = maturingServiceUpdater;
        this.tracer = tracer;
        this.eventGateway = eventGateway;
    }

    // TODO: Consider simplifying the case by removing the DB (always matches threshold)
    public Ingredients fetchIngredients(Order order, String processId, TestConfigurationHolder testConfigurationHolder) throws Exception {
        TestConfigurationHolder.TEST_CONFIG.set(testConfigurationHolder);
        log.info("Fetching ingredients for order [{}] , processId [{}], span [{}]", order, processId);
        /**
         * [SLEUTH] ParallelStreams won't work out of the box
         * - example of a completable future with our TraceableExecutorService
         * - makes little business sense here but that's just an example
         */
        CompletableFuture completableFuture = CompletableFuture.supplyAsync(() -> {
                    TestConfigurationHolder.TEST_CONFIG.set(testConfigurationHolder);
                    ingredientsCollector.collectIngredients(order, processId).stream()
                            .filter(ingredient -> ingredient != null)
                            .forEach((Ingredient ingredient) -> {
                                log.info("Adding an ingredient [{}] for order [{}] , processId [{}]", ingredient);
                                ingredientWarehouse.addIngredient(ingredient);
                            });
                    return null;
                }, new TraceableExecutorService(Executors.newFixedThreadPool(5),
                tracer,
                new SpanName("async", getClass().getSimpleName(), "method=fetchIngredients")));
        // block to perform the request (as I said the example is stupid)
        completableFuture.get();
        eventGateway.emitEvent(Event.builder().eventType(EventType.INGREDIENTS_ORDERED).processId(processId).build());
        Ingredients ingredients = ingredientWarehouse.getCurrentState();
        return maturingUpdater.updateIfLimitReached(ingredients, processId);
    }

}
