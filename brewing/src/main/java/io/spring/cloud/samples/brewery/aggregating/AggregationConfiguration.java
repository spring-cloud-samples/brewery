package io.spring.cloud.samples.brewery.aggregating;

import io.spring.cloud.samples.brewery.common.MaturingService;
import io.spring.cloud.samples.brewery.common.TestConfiguration;
import io.spring.cloud.samples.brewery.common.events.EventGateway;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

@Configuration
@Import(TestConfiguration.class)
class AggregationConfiguration {

	@Bean
	IngredientsProperties ingredientsProperties() {
		return new IngredientsProperties();
	}

	@Bean
	@LoadBalanced
	@ConditionalOnMissingBean
	public RestTemplate aggregationLoadBalancedRestTemplate() {
		return new RestTemplate();
	}

	@Bean
	MaturingServiceUpdater maturingServiceUpdater(IngredientsProperties ingredientsProperties,
												  IngredientWarehouse ingredientWarehouse,
												  MaturingService maturingService,
												  EventGateway eventGateway) {
		return new MaturingServiceUpdater(ingredientsProperties,
				ingredientWarehouse, maturingService, eventGateway);
	}

	@Bean
	IngredientsCollector ingredientsCollector(@LoadBalanced RestTemplate restTemplate,
											  IngredientsProxy ingredientsProxy) {
		return new IngredientsCollector(restTemplate, ingredientsProxy);
	}
}
