package io.spring.cloud.samples.brewery.ingredients;

import static io.spring.cloud.samples.brewery.common.TestConfigurationHolder.TEST_COMMUNICATION_TYPE_HEADER_NAME;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.sleuth.trace.TraceContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableDiscoveryClient
@RestController
@Slf4j
public class Application {

    @Autowired private IngredientsProperties ingredientsProperties;

	/**
     * [SLEUTH] WebAsyncTask
     */
    @RequestMapping(value = "/{ingredient}", method = RequestMethod.POST)
    public WebAsyncTask<Ingredient> ingredients(@PathVariable("ingredient") IngredientType ingredientType,
                                               @RequestHeader("PROCESS-ID") String processId,
                                               @RequestHeader(TEST_COMMUNICATION_TYPE_HEADER_NAME) String testCommunicationType) {
        log.info("Received a request to [/{}] with process id [{}] and communication type [{}] and trace id [{}]", ingredientType,
                processId, testCommunicationType, TraceContextHolder.getCurrentSpan().getTraceId());
        return new WebAsyncTask<>(() -> {
            Ingredient ingredient = new Ingredient(ingredientType, ingredientsProperties.getReturnedIngredientsQuantity());
            log.info("Returning [{}] as fetched ingredient from an external service. Trace id [{}]", ingredient, TraceContextHolder.getCurrentSpan().getTraceId());
            return ingredient;
        });
    }

    public static void main(String[] args) {
        new SpringApplication(Application.class).run(args);
    }
}
