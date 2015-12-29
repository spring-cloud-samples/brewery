package io.spring.cloud.samples.brewery.common;

import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;

@Configuration
@ComponentScan
@IntegrationComponentScan
public class TestConfiguration {

	@Bean
	public FilterRegistrationBean correlationIdFilter() {
		return new FilterRegistrationBean(new TestConfigurationSettingFilter());
	}

}
