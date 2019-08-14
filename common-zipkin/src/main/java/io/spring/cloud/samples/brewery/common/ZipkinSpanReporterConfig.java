package io.spring.cloud.samples.brewery.common;

import org.slf4j.Logger;

import org.springframework.cloud.sleuth.SpanAdjuster;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Marcin Grzejszczak
 */
@Configuration
public class ZipkinSpanReporterConfig {

	private static final Logger log = org.slf4j.LoggerFactory.getLogger(ZipkinSpanReporterConfig.class);

	@Bean SpanAdjuster loggingSpanAdjuster() {
		return span -> {
			log.info("Will report span [{}] to zipkin", span);
			return span;
		};
	}
}
