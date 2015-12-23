package io.spring.cloud.samples.brewery.aggregating;

import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.trace.TraceContextHolder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.spring.cloud.samples.brewery.aggregating.model.Ingredients;
import io.spring.cloud.samples.brewery.aggregating.model.Order;
import io.spring.cloud.samples.brewery.aggregating.model.Version;
import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(value = "/ingredients", consumes = Version.AGGREGATING_V1, produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class IngredientsController {

    private final IngredientsAggregator ingredientsAggregator;

    @Autowired
    public IngredientsController(IngredientsAggregator ingredientsAggregator) {
        this.ingredientsAggregator = ingredientsAggregator;
    }

	/**
     * [SLEUTH] Callable - separate thread pool
     */
    @RequestMapping(method = RequestMethod.POST)
    public Callable<Ingredients> distributeIngredients(@RequestBody Order order,
                                                       @RequestHeader("PROCESS-ID") String processId,
                                                       @RequestHeader(value = TestConfigurationHolder.TEST_COMMUNICATION_TYPE_HEADER_NAME,
                                                     defaultValue = "REST_TEMPLATE", required = false)
                                             TestConfigurationHolder.TestCommunicationType testCommunicationType) {
        log.info("Starting beer brewing process for process id [{}] and trace id [{}]", processId, TraceContextHolder.getCurrentSpan().getTraceId());
        TestConfigurationHolder testConfigurationHolder = TestConfigurationHolder.TEST_CONFIG.get();
        return () -> ingredientsAggregator.fetchIngredients(order, processId, testConfigurationHolder);
    }

}
