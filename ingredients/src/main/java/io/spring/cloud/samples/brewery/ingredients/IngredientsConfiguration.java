package io.spring.cloud.samples.brewery.ingredients;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(StubbedIngredientsProperties.class)
class IngredientsConfiguration {

	@Bean
	@LoadBalanced
	RestTemplate loadBalancedRestTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}

}
