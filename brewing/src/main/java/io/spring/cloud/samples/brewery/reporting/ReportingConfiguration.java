package io.spring.cloud.samples.brewery.reporting;

import io.spring.cloud.samples.brewery.common.events.EventSink;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBinding(EventSink.class)
class ReportingConfiguration {
}
