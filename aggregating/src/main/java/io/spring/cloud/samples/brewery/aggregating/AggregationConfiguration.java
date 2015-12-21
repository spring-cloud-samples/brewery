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
												MaturingServiceClient maturingServiceClient,
												@LoadBalanced RestTemplate restTemplate) {
		return new IngredientsAggregator(ingredientsProperties, ingredientWarehouse,
				maturingServiceClient, restTemplate);
	}
}

