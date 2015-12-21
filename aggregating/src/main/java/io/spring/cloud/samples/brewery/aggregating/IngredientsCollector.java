package io.spring.cloud.samples.brewery.aggregating;

import io.spring.cloud.samples.brewery.aggregating.model.Ingredient;
import io.spring.cloud.samples.brewery.aggregating.model.Order;
import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

import static io.spring.cloud.samples.brewery.common.TestConfigurationHolder.TestCommunicationType.FEIGN;
import static io.spring.cloud.samples.brewery.common.TestRequestEntityBuilder.requestEntity;

public class IngredientsCollector {

	private final RestTemplate restTemplate;
	private final IngredientsProxy ingredientsProxy;

	public IngredientsCollector(RestTemplate restTemplate, IngredientsProxy ingredientsProxy) {
		this.restTemplate = restTemplate;
		this.ingredientsProxy = ingredientsProxy;
	}

	List<Ingredient> collectIngredients(Order order, String processId) {
		switch (TestConfigurationHolder.TEST_CONFIG.get().getTestCommunicationType()) {
			case FEIGN:
				return callViaFeign(order, processId);
			default:
				return callViaRestTemplate(order, processId);
		}
	}

	private List<Ingredient> callViaFeign(Order order, String processId) {
		return order.getItems()
				.stream()
				.map(item -> ingredientsProxy.ingredients(item, processId, FEIGN.name()))
				.collect(Collectors.toList());
	}

	private List<Ingredient> callViaRestTemplate(Order order, String processId) {
		return order.getItems()
				.stream()
				.map(item ->
						restTemplate.exchange(requestEntity()
								.processId(processId)
								.serviceName("zuul")
								.url(item.name())
								.httpMethod(HttpMethod.POST)
								.build(), Ingredient.class).getBody()
				)
				.collect(Collectors.toList());
	}
}
