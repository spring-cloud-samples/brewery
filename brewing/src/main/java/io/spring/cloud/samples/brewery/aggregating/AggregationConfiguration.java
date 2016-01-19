package io.spring.cloud.samples.brewery.aggregating;

import io.spring.cloud.samples.brewery.common.MaturingService;
import io.spring.cloud.samples.brewery.common.events.EventGateway;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.sleuth.Tracer;
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
	IngredientsAggregator ingredientsAggregator(Tracer tracer,
												IngredientWarehouse ingredientWarehouse,
												MaturingServiceUpdater maturingServiceUpdater,
												IngredientsCollector ingredientsCollector,
												EventGateway eventGateway) {
		return new IngredientsAggregator(ingredientWarehouse,
				maturingServiceUpdater, ingredientsCollector, tracer, eventGateway);
	}

	@Bean
	MaturingServiceUpdater maturingServiceUpdater(IngredientsProperties ingredientsProperties,
												  IngredientWarehouse ingredientWarehouse,
												  MaturingService maturingService,
												  @LoadBalanced RestTemplate restTemplate,
												  EventGateway eventGateway) {
		return new MaturingServiceUpdater(ingredientsProperties,
				ingredientWarehouse, maturingService, restTemplate, eventGateway);
	}

	@Bean
	IngredientsCollector ingredientsCollector(@LoadBalanced RestTemplate restTemplate,
											  IngredientsProxy ingredientsProxy) {
		return new IngredientsCollector(restTemplate, ingredientsProxy);
	}
}

