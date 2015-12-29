package io.spring.cloud.samples.brewery.reporting;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReportingConfiguration {

	@Bean
	ReportingRepository reportingDatabase() {
		return new ReportingRepository();
	}

}
