package io.spring.cloud.samples.brewery.aggregating;

import java.util.concurrent.Callable;

import brave.Span;
import brave.Tracer;
import io.spring.cloud.samples.brewery.common.model.Ingredients;
import io.spring.cloud.samples.brewery.common.model.Order;
import io.spring.cloud.samples.brewery.common.model.Version;
import org.slf4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/ingredients", consumes = Version.BREWING_V1, produces = MediaType.APPLICATION_JSON_VALUE)
class IngredientsController {

	private static final Logger log = org.slf4j.LoggerFactory.getLogger(IngredientsController.class);
	private final IngredientsAggregator ingredientsAggregator;
	private final Tracer tracer;

	@Autowired
	public IngredientsController(IngredientsAggregator ingredientsAggregator, Tracer tracer) {
		this.ingredientsAggregator = ingredientsAggregator;
		this.tracer = tracer;
	}

	/**
	 * [OBSERVABILITY] Callable - separate thread pool
	 */
	@RequestMapping(method = RequestMethod.POST)
	public Callable<Ingredients> distributeIngredients(@RequestBody Order order,
			@RequestHeader("PROCESS-ID") String processId) {
		log.info("Setting tags and events on an already existing span");
		tracer.currentSpan().tag("beer", "stout");
		tracer.currentSpan().annotate("ingredientsAggregationStarted");
		log.info("Starting beer brewing process for process id [{}]", processId);
		Span span = tracer.nextSpan().name("inside_aggregating").start();
		try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
			return () -> ingredientsAggregator.fetchIngredients(order, processId);
		}
		finally {
			span.finish();
		}
	}

}
