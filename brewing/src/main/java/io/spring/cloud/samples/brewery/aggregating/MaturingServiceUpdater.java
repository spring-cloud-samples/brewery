package io.spring.cloud.samples.brewery.aggregating;

import io.spring.cloud.samples.brewery.common.MaturingService;
import io.spring.cloud.samples.brewery.common.model.IngredientType;
import io.spring.cloud.samples.brewery.common.model.Ingredients;
import io.spring.cloud.samples.brewery.common.events.Event;
import io.spring.cloud.samples.brewery.common.events.EventGateway;
import io.spring.cloud.samples.brewery.common.events.EventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.trace.SpanContextHolder;
import org.springframework.web.client.RestTemplate;

import static io.spring.cloud.samples.brewery.common.TestConfigurationHolder.TestCommunicationType.FEIGN;

@Slf4j
class MaturingServiceUpdater {

    private final IngredientsProperties ingredientsProperties;
    private final IngredientWarehouse ingredientWarehouse;
    private final MaturingService maturingService;
    private final RestTemplate restTemplate;
    private final EventGateway eventGateway;

    public MaturingServiceUpdater(IngredientsProperties ingredientsProperties,
                                  IngredientWarehouse ingredientWarehouse,
                                  MaturingService maturingService, RestTemplate restTemplate, EventGateway eventGateway) {
        this.ingredientsProperties = ingredientsProperties;
        this.ingredientWarehouse = ingredientWarehouse;
        this.maturingService = maturingService;
        this.restTemplate = restTemplate;
        this.eventGateway = eventGateway;
    }

    public Ingredients updateIfLimitReached(Ingredients ingredients, String processId) {
        if (ingredientsMatchTheThreshold(ingredients)) {
            log.info("Ingredients match the threshold [{}] - time to notify the maturing service! Span info [{}]",
                    ingredientsProperties.getThreshold(), SpanContextHolder.getCurrentSpan().getTraceId());
            eventGateway.emitEvent(Event.builder().eventType(EventType.BREWING_STARTED).processId(processId).build());
            notifyMaturingService(ingredients, processId);
            ingredientWarehouse.useIngredients(ingredientsProperties.getThreshold());
        }
        Ingredients currentState = ingredientWarehouse.getCurrentState();
        log.info("Current state of ingredients is [{}]. Current traceid [{}]", currentState, SpanContextHolder.getCurrentSpan().getTraceId());
        return currentState;
    }

    private boolean ingredientsMatchTheThreshold(Ingredients ingredients) {
        boolean allIngredientsPresent = ingredients.ingredients.size() == IngredientType.values().length;
        boolean allIngredientsOverThreshold =
                ingredients.ingredients.stream().allMatch(
                        ingredient -> ingredient.getQuantity() >= ingredientsProperties.getThreshold());
        return allIngredientsPresent && allIngredientsOverThreshold;
    }

    private void notifyMaturingService(Ingredients ingredients, String processId) {
        maturingService.distributeIngredients(ingredients, processId, FEIGN.name());
    }
}
