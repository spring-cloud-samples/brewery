package io.spring.cloud.samples.brewery.aggregating;

import io.spring.cloud.samples.brewery.common.TestConfiguration;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.sleuth.TraceManager;
import org.springframework.cloud.sleuth.zipkin.EndpointLocator;
import org.springframework.cloud.sleuth.zipkin.ServerPropertiesEndpointLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.util.SocketUtils;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Configuration
@Import(TestConfiguration.class)
class AggregationConfiguration {
    @Bean
    IngredientsProperties ingredientsProperties(@Value("${ingredients.rootUrl:}") String rootUrl) {
        IngredientsProperties ingredientsProperties = new IngredientsProperties();
        ingredientsProperties.setRootUrl(StringUtils.defaultIfBlank(rootUrl,
                "http://localhost:" + String.valueOf(SocketUtils.findAvailableTcpPort())));
        return ingredientsProperties;
    }

    @Bean(initMethod = "start", destroyMethod = "close")
    ExternalServicesStub externalServicesStub(IngredientsProperties  ingredientsProperties) throws IOException {
        return new ExternalServicesStub(ingredientsProperties);
    }

    @Bean
    AsyncRestTemplate asyncRestTemplate() {
        return new AsyncRestTemplate();
    }

    @Bean
    IngredientsAggregator ingredientsAggregator(IngredientsProperties ingredientsProperties,
                                                IngredientWarehouse ingredientWarehouse,
                                                TraceManager traceManager, AsyncRestTemplate asyncRestTemplate,
                                                MaturingServiceClient maturingServiceClient,
                                                @LoadBalanced RestTemplate restTemplate) {
        return new IngredientsAggregator(ingredientsProperties, ingredientWarehouse, traceManager,
                asyncRestTemplate, maturingServiceClient, restTemplate);
    }
}

