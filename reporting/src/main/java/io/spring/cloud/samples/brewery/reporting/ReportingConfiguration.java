package io.spring.cloud.samples.brewery.reporting;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import io.spring.cloud.samples.brewery.common.events.RabbitMqConfiguration;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Import(RabbitMqConfiguration.class)
@Slf4j
public class ReportingConfiguration {

	@Bean
	ReportingRepository reportingDatabase() {
		return new ReportingRepository();
	}

}
