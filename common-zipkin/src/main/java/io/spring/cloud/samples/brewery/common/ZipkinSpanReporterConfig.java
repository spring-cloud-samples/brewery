package io.spring.cloud.samples.brewery.common;

import brave.handler.FinishedSpanHandler;
import brave.handler.MutableSpan;
import brave.propagation.TraceContext;
import org.slf4j.Logger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Marcin Grzejszczak
 */
@Configuration
public class ZipkinSpanReporterConfig {

	private static final Logger log = org.slf4j.LoggerFactory.getLogger(ZipkinSpanReporterConfig.class);

	@Bean
	FinishedSpanHandler loggingSpanAdjuster() {
		return new FinishedSpanHandler() {
			@Override
			public boolean handle(TraceContext context, MutableSpan span) {
				log.info("Will report span [{}] to zipkin", span);
				return true;
			}
		};
	}
}
