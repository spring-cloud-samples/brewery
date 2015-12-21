package io.spring.cloud.samples.brewery.aggregating;

import static io.spring.cloud.samples.brewery.common.TestConfigurationHolder.TEST_COMMUNICATION_TYPE_HEADER_NAME;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.spring.cloud.samples.brewery.aggregating.model.Ingredient;
import io.spring.cloud.samples.brewery.aggregating.model.IngredientType;

@FeignClient("zuul")
public interface IngredientsProxy {

	@RequestMapping(value = "/{ingredient}", method = RequestMethod.POST)
	Ingredient ingredients(@PathVariable("ingredient") IngredientType ingredientType,
						   @RequestHeader("PROCESS-ID") String processId,
						   @RequestHeader(TEST_COMMUNICATION_TYPE_HEADER_NAME) String testCommunicationType);
}
