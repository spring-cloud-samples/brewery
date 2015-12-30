package io.spring.cloud.samples.brewery.aggregating;

import io.spring.cloud.samples.brewery.common.model.Ingredient;
import io.spring.cloud.samples.brewery.common.model.Ingredients;
import io.spring.cloud.samples.brewery.common.model.Order;
import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import io.spring.cloud.samples.brewery.common.events.Event;
import io.spring.cloud.samples.brewery.common.events.EventGateway;
import io.spring.cloud.samples.brewery.common.events.EventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.TraceManager;
import org.springframework.cloud.sleuth.instrument.executor.TraceableExecutorService;
import org.springframework.cloud.sleuth.trace.TraceContextHolder;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

@Slf4j
class IngredientsAggregator {

    private final MaturingServiceUpdater maturingUpdater;
    private final IngredientWarehouse ingredientWarehouse;
    private final IngredientsCollector ingredientsCollector;
    private final TraceManager traceManager;
    private final EventGateway eventGateway;

    IngredientsAggregator(IngredientWarehouse ingredientWarehouse,
                          MaturingServiceUpdater maturingServiceUpdater,
                          IngredientsCollector ingredientsCollector,
                          TraceManager traceManager, EventGateway eventGateway) {
        this.ingredientWarehouse = ingredientWarehouse;
        this.ingredientsCollector = ingredientsCollector;
        this.maturingUpdater = maturingServiceUpdater;
        this.traceManager = traceManager;
        this.eventGateway = eventGateway;
    }

    // TODO: Consider simplifying the case by removing the DB (always matches threshold)

	/**
     * [SLEUTH] Async
     */
    @Async
    public Ingredients fetchIngredients(Order order, String processId, TestConfigurationHolder testConfigurationHolder) throws ExecutionException, InterruptedException {
        TestConfigurationHolder.TEST_CONFIG.set(testConfigurationHolder);
        log.info("Fetching ingredients for order [{}] , processId [{}], traceid [{}]", order, processId, TraceContextHolder.isTracing() ?
                TraceContextHolder.getCurrentSpan().getTraceId() : "");
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
                                log.info("Adding an ingredient for order [{}] , processId [{}], traceid [{}]", order, processId, TraceContextHolder.isTracing() ?
                                        TraceContextHolder.getCurrentSpan().getTraceId() : "");
                                ingredientWarehouse.addIngredient(ingredient);
                            });
                    return null;
                }, new TraceableExecutorService(Executors.newFixedThreadPool(5), traceManager));
        // block to perform the request (as I said the example is stupid)
        completableFuture.get();
        eventGateway.emitEvent(Event.builder().eventType(EventType.INGREDIENTS_ORDERED).processId(processId).build());
        Ingredients ingredients = ingredientWarehouse.getCurrentState();
        return maturingUpdater.updateIfLimitReached(ingredients, processId);
    }

}
