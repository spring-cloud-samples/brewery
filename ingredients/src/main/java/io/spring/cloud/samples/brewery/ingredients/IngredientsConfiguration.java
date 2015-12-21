package io.spring.cloud.samples.brewery.ingredients;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IngredientsConfiguration {

	@Bean IngredientsProperties ingredientsProperties() {
		return new IngredientsProperties();
	}
}
