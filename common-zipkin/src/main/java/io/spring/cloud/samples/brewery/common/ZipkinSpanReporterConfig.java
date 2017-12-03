package io.spring.cloud.samples.brewery.common;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.sleuth.SpanReporter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Marcin Grzejszczak
 */
@Configuration
class ZipkinSpanReporterConfig {

	@Bean BeanPostProcessor zipkinSpanBPP() {
		return new BeanPostProcessor() {
			@Override public Object postProcessBeforeInitialization(Object bean,
					String beanName) throws BeansException {
				return bean;
			}

			@Override public Object postProcessAfterInitialization(Object bean,
					String beanName) throws BeansException {
				if (bean instanceof SpanReporter && !(bean instanceof StoringZipkinSpanReporter)) {
					return new StoringZipkinSpanReporter((SpanReporter) bean);
				}
				return bean;
			}
		};
	}
}
