package io.spring.cloud.samples.brewery.common;

import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.DispatcherType;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.web.observation.HttpRequestsObservationFilter;

@Configuration
@ComponentScan
@IntegrationComponentScan
public class TestConfiguration {

	// Spring Mvc
	@Bean
	FilterRegistrationBean traceWebFilter(ObservationRegistry observationRegistry) {
		FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(new HttpRequestsObservationFilter(observationRegistry));
		filterRegistrationBean.setDispatcherTypes(DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.FORWARD,
				DispatcherType.INCLUDE, DispatcherType.REQUEST);
		filterRegistrationBean.setOrder(Ordered.LOWEST_PRECEDENCE);
		return filterRegistrationBean;
	}

}
