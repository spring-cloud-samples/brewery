package io.spring.cloud.samples.brewery.aggregating;

import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import io.spring.cloud.samples.brewery.common.model.Ingredients;
import io.spring.cloud.samples.brewery.common.model.Order;
import io.spring.cloud.samples.brewery.common.model.Version;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import brave.Span;
import brave.Tracer;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Callable;

@RestController
@RequestMapping(value = "/ingredients", consumes = Version.BREWING_V1, produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
class IngredientsController {

    private final IngredientsAggregator ingredientsAggregator;
    private final Tracer tracer;

    @Autowired
    public IngredientsController(IngredientsAggregator ingredientsAggregator, Tracer tracer) {
        this.ingredientsAggregator = ingredientsAggregator;
        this.tracer = tracer;
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
        log.info("Setting tags and events on an already existing span");
        tracer.currentSpan().tag("beer", "stout");
        tracer.currentSpan().annotate("ingredientsAggregationStarted");
        log.info("Starting beer brewing process for process id [{}]", processId);
        Span span = tracer.nextSpan().name("inside_aggregating").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            TestConfigurationHolder testConfigurationHolder = TestConfigurationHolder.TEST_CONFIG.get();
            return () -> ingredientsAggregator.fetchIngredients(order, processId, testConfigurationHolder);
        } finally {
            span.finish();
        }
    }

}
