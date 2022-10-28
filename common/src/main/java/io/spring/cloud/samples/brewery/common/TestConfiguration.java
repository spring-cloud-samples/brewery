package io.spring.cloud.samples.brewery.common;

import io.micrometer.tracing.otel.bridge.OtelBaggageManager;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.propagation.BaggageTextMapPropagator;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.springframework.boot.actuate.autoconfigure.tracing.TracingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;

import java.util.Collections;
import java.util.List;

@Configuration
@ComponentScan
@IntegrationComponentScan
public class TestConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(OtelCurrentTraceContext.class)
    static class OtelConfig {

        private TextMapPropagator otelRemoteFieldsBaggageTextMapPropagator(TracingProperties tracingProperties, OtelCurrentTraceContext otelCurrentTraceContext) {
            List<String> remoteFields = tracingProperties.getBaggage().getRemoteFields();
            return new BaggageTextMapPropagator(remoteFields,
                    new OtelBaggageManager(otelCurrentTraceContext, remoteFields, Collections.emptyList()));
        }

        // TODO: Remove me after Boot config gets fixed via https://github.com/spring-projects/spring-boot/pull/32898
        @Bean
        ContextPropagators myW3cContextPropagators(TracingProperties tracingProperties, OtelCurrentTraceContext otelCurrentTraceContext) {
            return ContextPropagators.create(TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(),
                    W3CBaggagePropagator.getInstance(), otelRemoteFieldsBaggageTextMapPropagator(tracingProperties, otelCurrentTraceContext)));
        }
    }

}
