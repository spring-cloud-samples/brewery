package io.spring.cloud.samples.brewery.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.SpanAdjuster;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Marcin Grzejszczak
 */
@Configuration
@Slf4j
public class ZipkinSpanReporterConfig {

	@Bean SpanAdjuster loggingSpanAdjuster() {
		return span -> {
			log.info("Will report span [{}] to zipkin", span);
			return span;
		};
	}
}
