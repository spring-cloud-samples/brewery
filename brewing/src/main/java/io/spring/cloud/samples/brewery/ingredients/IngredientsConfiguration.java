package io.spring.cloud.samples.brewery.ingredients;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class IngredientsConfiguration {

	@Bean
	StubbedIngredientsProperties stubbedIngredientsProperties() {
		return new StubbedIngredientsProperties();
	}
}
