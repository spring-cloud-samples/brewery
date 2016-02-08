package io.spring.cloud.samples.brewery.ingredients;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanName;
import org.springframework.cloud.sleuth.Tracer;
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
	private final Tracer tracer;

	@Autowired
	IngredientsFetchController(StubbedIngredientsProperties stubbedIngredientsProperties, Tracer tracer) {
		this.stubbedIngredientsProperties = stubbedIngredientsProperties;
		this.tracer = tracer;
	}

	/**
	 * [SLEUTH] WebAsyncTask
	 */
	@RequestMapping(value = "/ingredient/{ingredient}", method = RequestMethod.POST)
	public WebAsyncTask<Ingredient> ingredients(@PathVariable("ingredient") IngredientType ingredientType,
												@RequestHeader("PROCESS-ID") String processId,
												@RequestHeader(TestConfigurationHolder.TEST_COMMUNICATION_TYPE_HEADER_NAME) String testCommunicationType) {
		log.info("Received a request to [/{}] with process id [{}] and communication type [{}]", ingredientType,
				processId, testCommunicationType);
		return new WebAsyncTask<>(() -> {
			Span span = tracer.startTrace(new SpanName("local", "inside_ingredients"));
			Ingredient ingredient = new Ingredient(ingredientType, stubbedIngredientsProperties.getReturnedIngredientsQuantity());
			log.info("Returning [{}] as fetched ingredient from an external service", ingredient);
			tracer.close(span);
			return ingredient;
		});
	}
}
