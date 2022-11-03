package io.spring.cloud.samples.brewery.ingredients;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.spring.cloud.samples.brewery.common.model.Ingredient;
import io.spring.cloud.samples.brewery.common.model.IngredientType;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
class IngredientsFetchController {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(IngredientsFetchController.class);
    private final StubbedIngredientsProperties stubbedIngredientsProperties;
    private final ObservationRegistry observationRegistry;

    IngredientsFetchController(StubbedIngredientsProperties stubbedIngredientsProperties, ObservationRegistry observationRegistry) {
        this.stubbedIngredientsProperties = stubbedIngredientsProperties;
        this.observationRegistry = observationRegistry;
    }

    /**
     * [OBSERVABILITY] Mono
     */
    @RequestMapping(value = "/{ingredient}", method = RequestMethod.POST)
    public Mono<Ingredient> ingredients(@PathVariable("ingredient") IngredientType ingredientType,
            @RequestHeader("PROCESS-ID") String processId,
            @RequestHeader("TEST-COMMUNICATION-TYPE") String testCommunicationType) {
        log.info("Received a request to [/{}] with process id [{}] and communication type [{}]", ingredientType,
                processId, testCommunicationType);
        Observation insideIngredientsObservation = Observation.start("inside_ingredients", observationRegistry);
        Ingredient ingredient = new Ingredient(ingredientType, stubbedIngredientsProperties.getReturnedIngredientsQuantity());
        log.info("Returning [{}] as fetched ingredient from an external service", ingredient);
        return Mono.just(ingredient)
                .contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, insideIngredientsObservation))
                .doFinally(signalType -> insideIngredientsObservation.stop());
    }
}
