package io.spring.cloud.samples.brewery.aggregating;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.springframework.cloud.sleuth.TraceManager;
import org.springframework.cloud.sleuth.instrument.executor.TraceableExecutorService;
import org.springframework.cloud.sleuth.trace.TraceContextHolder;
import org.springframework.scheduling.annotation.Async;

import io.spring.cloud.samples.brewery.aggregating.model.Ingredient;
import io.spring.cloud.samples.brewery.aggregating.model.Ingredients;
import io.spring.cloud.samples.brewery.aggregating.model.Order;
import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class IngredientsAggregator {

    private final MaturingServiceUpdater maturingUpdater;
    private final IngredientWarehouse ingredientWarehouse;
    private final IngredientsCollector ingredientsCollector;
    private final TraceManager traceManager;

    IngredientsAggregator(IngredientWarehouse ingredientWarehouse,
                          MaturingServiceUpdater maturingServiceUpdater,
                          IngredientsCollector ingredientsCollector,
                          TraceManager traceManager) {
        this.ingredientWarehouse = ingredientWarehouse;
        this.ingredientsCollector = ingredientsCollector;
        this.maturingUpdater = maturingServiceUpdater;
        this.traceManager = traceManager;
    }

    // TODO: Consider simplifying the case by removing the DB (always matches threshold)

	/**
     * [SLEUTH] Async
     */
    @Async
    public Ingredients fetchIngredients(Order order, String processId, TestConfigurationHolder testConfigurationHolder) throws ExecutionException, InterruptedException {
        TestConfigurationHolder.TEST_CONFIG.set(testConfigurationHolder);
        log.info("Fetching ingredients for order [{}] , processId [{}], traceid [{}]", order, processId, TraceContextHolder.getCurrentSpan().getTraceId());

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
                                log.info("Adding an ingredient for order [{}] , processId [{}], traceid [{}]", order, processId, TraceContextHolder.getCurrentSpan().getTraceId());
                                ingredientWarehouse.addIngredient(ingredient);
                            });
                    return null;
                }, new TraceableExecutorService(Executors.newFixedThreadPool(5), traceManager));
        // block to perform the request (as I said the example is stupid)
        completableFuture.get();
        Ingredients ingredients = ingredientWarehouse.getCurrentState();
        return maturingUpdater.updateIfLimitReached(ingredients, processId);
    }

}
