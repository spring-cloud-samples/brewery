package io.spring.cloud.samples.brewery.acceptance.common.tech

import groovy.util.logging.Slf4j
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.RetryListener
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.TimeoutRetryPolicy
import org.springframework.retry.support.RetryTemplate

import static java.util.concurrent.TimeUnit.SECONDS

@Slf4j
class ExceptionLoggingRetryTemplate extends RetryTemplate {

	ExceptionLoggingRetryTemplate(Integer timeout) {
		retryPolicy = new TimeoutRetryPolicy(timeout: SECONDS.toMillis(timeout))
		backOffPolicy = new FixedBackOffPolicy()
		registerListener(new RetryListener() {
			@Override
			def <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
				return true
			}

			@Override
			def <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {

			}

			@Override
			def <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
				log.error("Exception occurred while trying to send a message", throwable)
			}
		})
	}
}
