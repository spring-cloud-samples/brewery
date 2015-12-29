package io.spring.cloud.samples.brewery.aggregating;

import io.spring.cloud.samples.brewery.aggregating.model.IngredientType;
import io.spring.cloud.samples.brewery.aggregating.model.Ingredients;
import io.spring.cloud.samples.brewery.aggregating.model.Version;
import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import io.spring.cloud.samples.brewery.common.events.Event;
import io.spring.cloud.samples.brewery.common.events.EventGateway;
import io.spring.cloud.samples.brewery.common.events.EventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import static io.spring.cloud.samples.brewery.common.TestConfigurationHolder.TestCommunicationType.FEIGN;
import static io.spring.cloud.samples.brewery.common.TestRequestEntityBuilder.requestEntity;

@Slf4j
class MaturingServiceUpdater {

    private final IngredientsProperties ingredientsProperties;
    private final IngredientWarehouse ingredientWarehouse;
    private final MaturingServiceClient maturingServiceClient;
    private final RestTemplate restTemplate;
    private final EventGateway eventGateway;

    public MaturingServiceUpdater(IngredientsProperties ingredientsProperties,
                                  IngredientWarehouse ingredientWarehouse,
                                  MaturingServiceClient maturingServiceClient, RestTemplate restTemplate, EventGateway eventGateway) {
        this.ingredientsProperties = ingredientsProperties;
        this.ingredientWarehouse = ingredientWarehouse;
        this.maturingServiceClient = maturingServiceClient;
        this.restTemplate = restTemplate;
        this.eventGateway = eventGateway;
    }

    public Ingredients updateIfLimitReached(Ingredients ingredients, String processId) {
        if (ingredientsMatchTheThreshold(ingredients)) {
            log.info("Ingredients match the threshold [{}] - time to notify the maturing service!",
                    ingredientsProperties.getThreshold());
            eventGateway.emitEvent(Event.builder().eventType(EventType.BREWING_STARTED).processId(processId).build());
            notifyMaturingService(ingredients, processId);
            ingredientWarehouse.useIngredients(ingredientsProperties.getThreshold());
        }
        Ingredients currentState = ingredientWarehouse.getCurrentState();
        log.info("Current state of ingredients is {}", currentState);
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
        switch (TestConfigurationHolder.TEST_CONFIG.get().getTestCommunicationType()) {
            case FEIGN:
                callViaFeign(ingredients, processId);
                break;
            default:
                useRestTemplateToCallAggregation(ingredients, processId);
        }
    }

    private void callViaFeign(Ingredients ingredients, String processId) {
        maturingServiceClient.distributeIngredients(ingredients, processId, FEIGN.name());
    }

    private void useRestTemplateToCallAggregation(Ingredients body, String processId) {
        restTemplate.exchange(requestEntity()
                .processId(processId)
                .contentTypeVersion(Version.MATURING_V1)
                .serviceName("maturing")
                .url("brew")
                .httpMethod(HttpMethod.POST)
                .body(body)
                .build(), String.class);
    }
}
