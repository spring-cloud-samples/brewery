package io.spring.cloud.samples.brewery.aggregating;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import io.spring.cloud.samples.brewery.common.TestConfiguration;

@Configuration
@Import(TestConfiguration.class)
class AggregationConfiguration {

	@Bean
	IngredientsProperties ingredientsProperties() {
		return new IngredientsProperties();
	}

	@Bean
	AsyncRestTemplate asyncRestTemplate() {
		return new AsyncRestTemplate();
	}

	@Bean
	IngredientsAggregator ingredientsAggregator(IngredientsProperties ingredientsProperties,
												IngredientWarehouse ingredientWarehouse,
												MaturingServiceUpdater maturingServiceUpdater,
												IngredientsCollector ingredientsCollector) {
		return new IngredientsAggregator(ingredientsProperties, ingredientWarehouse,
				maturingServiceUpdater, ingredientsCollector);
	}

	@Bean
	MaturingServiceUpdater maturingServiceUpdater(IngredientsProperties ingredientsProperties,
												  IngredientWarehouse ingredientWarehouse,
												  MaturingServiceClient maturingServiceClient,
												  @LoadBalanced RestTemplate restTemplate) {
		return new MaturingServiceUpdater(ingredientsProperties,
				ingredientWarehouse, maturingServiceClient, restTemplate);
	}

	@Bean
	IngredientsCollector ingredientsCollector(@LoadBalanced RestTemplate restTemplate,
											  IngredientsProxy ingredientsProxy) {
		return new IngredientsCollector(restTemplate, ingredientsProxy);
	}
}

