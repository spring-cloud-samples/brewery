/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.spring.cloud.samples.brewery.acceptance.common

import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import io.spring.cloud.samples.brewery.acceptance.common.sleuth.SleuthHashing
import io.spring.cloud.samples.brewery.acceptance.common.tech.ExceptionLoggingRestTemplate
import io.spring.cloud.samples.brewery.acceptance.common.tech.ExceptionLoggingRetryTemplate
import io.spring.cloud.samples.brewery.acceptance.common.tech.ServiceUrlFetcher
import io.spring.cloud.samples.brewery.acceptance.common.tech.TestConfiguration
import io.spring.cloud.samples.brewery.acceptance.model.CommunicationType
import io.spring.cloud.samples.brewery.acceptance.model.IngredientType
import io.spring.cloud.samples.brewery.acceptance.model.Order
import io.spring.cloud.samples.brewery.acceptance.model.ProcessState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.boot.test.WebIntegrationTest
import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.http.*
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.util.JdkIdGenerator
import org.springframework.web.client.RestTemplate
import spock.lang.Specification


/**
 *  TODO: Split responsibilities
 */
@ContextConfiguration(classes = TestConfiguration, loader = SpringApplicationContextLoader)
@WebIntegrationTest(randomPort = true)
@Slf4j
abstract class AbstractBreweryAcceptanceSpec extends Specification implements SleuthHashing {

	public static final String TRACE_ID_HEADER_NAME = 'X-TRACE-ID'
	public static final String SPAN_ID_HEADER_NAME = 'X-SPAN-ID'

	@Autowired ServiceUrlFetcher serviceUrlFetcher
	@Autowired(required = false) @LoadBalanced RestTemplate loadBalanced
	@Value('${presenting.timeout:30}') Integer timeout
	@Value('${LOCAL_URL:http://localhost}') String zipkinQueryUrl

	Runnable beer_has_been_brewed_for_process_id(String processId) {
		return new Runnable() {
			@Override
			void run() {
				ResponseEntity<String> process = checkStateOfTheProcess(processId)
				assert process.statusCode == HttpStatus.OK
				assert stateFromJson(process) == ProcessState.DONE.name()
			}
		}
	}

	Runnable beer_has_been_brewed_for_process_and_trace_id(String processId) {
		return new Runnable() {
			@Override
			void run() {
				ResponseEntity<String> process = checkStateOfTheProcess(processId)
				assert process.statusCode == HttpStatus.OK
				assert stateFromJson(process) == ProcessState.DONE.name()
			}
		}
	}

	Runnable entry_for_trace_id_is_present_in_Zipkin(String traceId) {
		return new Runnable() {
			@Override
			void run() {
				ResponseEntity<String> response = checkStateOfTheTraceId(traceId)
				assert response.statusCode == HttpStatus.OK
				assert response.hasBody()
				assert ['presenting', 'maturing', 'bottling', 'aggregating'].every {
						response.body.contains(it)
					}
			}
		}
	}

	ResponseEntity<String> checkStateOfTheProcess(String processId) {
		URI uri = URI.create("${serviceUrlFetcher.presentingServiceUrl()}/feed/process/$processId")
		HttpHeaders headers = new HttpHeaders()
		return new ExceptionLoggingRetryTemplate(timeout).execute(
				new RetryCallback<ResponseEntity<String>, Exception>() {
					@Override
					ResponseEntity<String> doWithRetry(RetryContext retryContext) throws Exception {
						return restTemplate().exchange(
								new RequestEntity<>(headers, HttpMethod.GET, uri), String
						)
					}
				}
		)
	}

	ResponseEntity<String> checkStateOfTheTraceId(String traceId) {
		String hexTraceId = convertToTraceIdZipkinRequest(traceId)
		URI uri = URI.create("${wrapQueryWithProtocolIfPresent() ?: zipkinQueryUrl}:9411/api/v1/trace/$hexTraceId")
		log.info("Performing a request for trace id [$traceId] and hex version [$hexTraceId]")
		HttpHeaders headers = new HttpHeaders()
		return new ExceptionLoggingRetryTemplate(timeout).execute(
				new RetryCallback<ResponseEntity<String>, Exception>() {
					@Override
					ResponseEntity<String> doWithRetry(RetryContext retryContext) throws Exception {
						return new ExceptionLoggingRestTemplate().exchange(
								new RequestEntity<>(headers, HttpMethod.GET, uri), String
						)
					}
				}
		)
	}

	String wrapQueryWithProtocolIfPresent() {
		String zipkinUrlFromEnvs = System.getenv('spring.zipkin.query.url')
		if (zipkinUrlFromEnvs) {
			return "http://$zipkinUrlFromEnvs"
		}
		return zipkinUrlFromEnvs
	}

	String stateFromJson(ResponseEntity<String> process) {
		return new JsonSlurper().parseText(process.body).state.toUpperCase()
	}

	String trace_id_header(HttpEntity httpEntity) {
		return httpEntity.headers.getFirst(TRACE_ID_HEADER_NAME)
	}

	RequestEntity an_order_for_all_ingredients_with_process_id(String processId, CommunicationType communicationType) {
		HttpHeaders headers = new HttpHeaders()
		headers.add("PROCESS-ID", processId)
		headers.add(TRACE_ID_HEADER_NAME, processId)
		headers.add(SPAN_ID_HEADER_NAME, new JdkIdGenerator().generateId().toString())
		headers.add("TEST-COMMUNICATION-TYPE", communicationType.name())
		URI uri = URI.create("${serviceUrlFetcher.presentingServiceUrl()}/present/order")
		log.info("Request with order for all ingredients to presenting service for uri [$uri] with headers [$headers] is ready")
		return new RequestEntity<>(allIngredients(), headers, HttpMethod.POST, uri)
	}

	ResponseEntity<String> presenting_service_has_been_called(RequestEntity requestEntity) {
		return new ExceptionLoggingRetryTemplate(timeout).execute(
				new RetryCallback<ResponseEntity<String>, Exception>() {
					@Override
					ResponseEntity<String> doWithRetry(RetryContext retryContext) throws Exception {
						return restTemplate().exchange(requestEntity, String)
					}
				}
		)
	}

	Order allIngredients() {
		return new Order(items: IngredientType.values())
	}

	RestTemplate restTemplate() {
		if (System.getProperty(ServiceUrlFetcher.LOCAL_MODE_PROP) ||
				System.getProperty(ServiceUrlFetcher.LOCAL_MODE_URL_PROP)) {
			return new ExceptionLoggingRestTemplate()
		}
		return loadBalanced ?: new ExceptionLoggingRestTemplate()
	}

}
