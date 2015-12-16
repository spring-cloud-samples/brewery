package io.spring.cloud.samples.brewery.eureka;

import io.spring.cloud.samples.brewery.eureka.model.Ingredients;
import io.spring.cloud.samples.brewery.eureka.model.Order;
import io.spring.cloud.samples.brewery.eureka.model.Version;
import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/ingredients", consumes = Version.AGGREGATING_V1, produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class IngredientsController {

    private final IngredientsAggregator ingredientsAggregator;

    @Autowired
    public IngredientsController(IngredientsAggregator ingredientsAggregator) {
        this.ingredientsAggregator = ingredientsAggregator;
    }

    @RequestMapping(method = RequestMethod.POST)
    public Ingredients distributeIngredients(@RequestBody Order order,
                                             @RequestHeader("PROCESS-ID") String processId,
                                             @RequestHeader(value = TestConfigurationHolder.TEST_COMMUNICATION_TYPE_HEADER_NAME,
                                                     defaultValue = "REST_TEMPLATE", required = false)
                                             TestConfigurationHolder.TestCommunicationType testCommunicationType) {
        log.info("Starting process for process id [{}]", processId);
        return ingredientsAggregator.fetchIngredients(order, processId);
    }

}
