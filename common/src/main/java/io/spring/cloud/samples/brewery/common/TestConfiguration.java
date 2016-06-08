package io.spring.cloud.samples.brewery.common;

import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.cloud.sleuth.Sampler;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.cloud.sleuth.util.ExceptionUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;

import javax.annotation.PostConstruct;

@Configuration
@ComponentScan
@IntegrationComponentScan
public class TestConfiguration {

	@Bean
	public FilterRegistrationBean correlationIdFilter() {
		return new FilterRegistrationBean(new TestConfigurationSettingFilter());
	}

	@Bean
	Sampler defaultSampler() {
		return new AlwaysSampler();
	}

	@PostConstruct
	public void setup() {
		ExceptionUtils.setFail(true);
	}

}
