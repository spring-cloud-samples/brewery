package io.spring.cloud.samples.brewery.common;

import org.springframework.cloud.sleuth.SpanReporter;
import org.springframework.cloud.sleuth.stream.StreamSpanReporter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author Marcin Grzejszczak
 */
@Configuration
public class ZipkinStreamConfiguration {

	@Bean
	@Primary
	public SpanReporter storingZipkinStreamSpanReporter(StreamSpanReporter streamSpanReporter) {
		return new StoringZipkinStreamSpanReporter(streamSpanReporter);
	}
}
