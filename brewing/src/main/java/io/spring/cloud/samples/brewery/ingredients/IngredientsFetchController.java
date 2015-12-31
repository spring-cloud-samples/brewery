package io.spring.cloud.samples.brewery.ingredients;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Trace;
import org.springframework.cloud.sleuth.TraceManager;
import org.springframework.cloud.sleuth.trace.TraceContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import io.spring.cloud.samples.brewery.common.model.Ingredient;
import io.spring.cloud.samples.brewery.common.model.IngredientType;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
class IngredientsFetchController {
	private final StubbedIngredientsProperties stubbedIngredientsProperties;
	private final TraceManager traceManager;

	@Autowired
	IngredientsFetchController(StubbedIngredientsProperties stubbedIngredientsProperties, TraceManager traceManager) {
		this.stubbedIngredientsProperties = stubbedIngredientsProperties;
		this.traceManager = traceManager;
	}

	/**
	 * [SLEUTH] WebAsyncTask
	 */
	@RequestMapping(value = "/ingredient/{ingredient}", method = RequestMethod.POST)
	public WebAsyncTask<Ingredient> ingredients(@PathVariable("ingredient") IngredientType ingredientType,
												@RequestHeader("PROCESS-ID") String processId,
												@RequestHeader(TestConfigurationHolder.TEST_COMMUNICATION_TYPE_HEADER_NAME) String testCommunicationType) {
		log.info("Received a request to [/{}] with process id [{}] and communication type [{}] and trace id [{}]", ingredientType,
				processId, testCommunicationType, TraceContextHolder.isTracing() ?
						TraceContextHolder.getCurrentSpan().getTraceId() : "");
		return new WebAsyncTask<>(() -> {
			Trace trace = traceManager.startSpan("inside_ingredients");
			Ingredient ingredient = new Ingredient(ingredientType, stubbedIngredientsProperties.getReturnedIngredientsQuantity());
			log.info("Returning [{}] as fetched ingredient from an external service. Span [{}]", ingredient, TraceContextHolder.isTracing() ?
					TraceContextHolder.getCurrentSpan() : "");
			traceManager.close(trace);
			return ingredient;
		});
	}
}
