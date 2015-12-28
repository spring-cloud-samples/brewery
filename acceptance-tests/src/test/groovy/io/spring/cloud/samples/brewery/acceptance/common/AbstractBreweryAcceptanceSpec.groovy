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
import io.spring.cloud.samples.brewery.acceptance.common.sleuth.SleuthHashing
import io.spring.cloud.samples.brewery.acceptance.common.tech.ExceptionLoggingRestTemplate
import io.spring.cloud.samples.brewery.acceptance.common.tech.ServiceUrlFetcher
import io.spring.cloud.samples.brewery.acceptance.common.tech.TestConfiguration
import io.spring.cloud.samples.brewery.acceptance.model.CommunicationType
import io.spring.cloud.samples.brewery.acceptance.model.IngredientType
import io.spring.cloud.samples.brewery.acceptance.model.Order
import io.spring.cloud.samples.brewery.acceptance.model.ProcessState
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.http.*
import org.springframework.test.context.ContextConfiguration
import org.springframework.util.JdkIdGenerator
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

import static com.jayway.awaitility.Awaitility.await
import static java.util.concurrent.TimeUnit.SECONDS

/**
 *  TODO: Split responsibilities
 */
@ContextConfiguration(classes = TestConfiguration, loader = SpringApplicationContextLoader)
abstract class AbstractBreweryAcceptanceSpec extends Specification implements SleuthHashing {

	public static final String TRACE_ID_HEADER_NAME = 'X-TRACE-ID'
	public static final String SPAN_ID_HEADER_NAME = 'X-SPAN-ID'
	public static final Logger log = LoggerFactory.getLogger(AbstractBreweryAcceptanceSpec)

	private static final List<String> APPS_NAMES_AND_PORTS_IN_ZIPKIN = ['presenting:9991', 'maturing:9993', 'bottling:9994',
															  'aggregating:9992', ':9995', 'ingredients:9996', 'reporting:9997']

	@Autowired ServiceUrlFetcher serviceUrlFetcher
	@Value('${presenting.poll.interval:1}') Integer pollInterval
	@Value('${presenting.timeout:30}') Integer timeout
	@Value('${zipkin.query.port:9411}') Integer zipkinQueryPort
	@Value('${LOCAL_URL:http://localhost}') String zipkinQueryUrl

	def setup() {
		log.info("Starting test")
	}

	def cleanup() {
		log.info("Finished test")
	}

	void beer_has_been_brewed_for_process_id(String processId) {
		await().pollInterval(pollInterval, SECONDS).atMost(timeout, SECONDS).until(new Runnable() {
			@Override
			void run() {
				ResponseEntity<String> process = checkStateOfTheProcess(processId)
				log.info("Response from the presenting service about the process state [$process] for process with id [$processId]")
				assert process.statusCode == HttpStatus.OK
				assert stateFromJson(process) == ProcessState.DONE.name()
				log.info("Beer has been successfully brewed! Service discovery is working! Let's be happy!")
			}
		})
	}

	void entry_for_trace_id_is_present_in_Zipkin(String traceId) {
		await().pollInterval(pollInterval, SECONDS).atMost(timeout, SECONDS).until(new Runnable() {
			@Override
			void run() {
				ResponseEntity<String> response = checkStateOfTheTraceId(traceId)
				log.info("Response from the Zipkin query service about the trace id [$response] for trace with id [$traceId]")
				assert response.statusCode == HttpStatus.OK
				assert response.hasBody()
				assert APPS_NAMES_AND_PORTS_IN_ZIPKIN.every {
						response.body.contains(it)
					}
				log.info("Zipkin tracing is working! Sleuth is working! Let's be happy!")
			}
		})
	}

	ResponseEntity<String> checkStateOfTheProcess(String processId) {
		URI uri = URI.create("${serviceUrlFetcher.presentingServiceUrl()}/feed/process/$processId")
		log.info("Sending request to the presenting service [$uri] to check the beer brewing process. The process id is [$processId]")
		return restTemplate().exchange(
				new RequestEntity<>(new HttpHeaders(), HttpMethod.GET, uri), String
		)
	}

	ResponseEntity<String> checkStateOfTheTraceId(String traceId) {
		String hexTraceId = convertToTraceIdZipkinRequest(traceId)
		URI uri = URI.create("${wrapQueryWithProtocolIfPresent() ?: zipkinQueryUrl}:${zipkinQueryPort}/api/v1/trace/$hexTraceId")
		HttpHeaders headers = new HttpHeaders()
		log.info("Sending request to the Zipkin query service [$uri]. Checking presence of trace id [$traceId] and its hex version [$hexTraceId]")
		return new ExceptionLoggingRestTemplate().exchange(
				new RequestEntity<>(headers, HttpMethod.GET, uri), String
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
		Order allIngredients = allIngredients()
		RequestEntity requestEntity = new RequestEntity<>(allIngredients, headers, HttpMethod.POST, uri)
		log.info("Request with order for all ingredients to presenting service [$requestEntity] is ready")
		return requestEntity
	}

	void presenting_service_has_been_called(RequestEntity requestEntity) {
		await().pollInterval(pollInterval, SECONDS).atMost(timeout, SECONDS).until(new Runnable() {
				@Override
				void run() {
					log.info("Sending [$requestEntity] to start brewing the beer.")
					ResponseEntity<String> responseEntity = restTemplate().exchange(requestEntity, String)
					log.info("Received [$responseEntity] from the presenting service.")
					assert responseEntity.statusCode == HttpStatus.OK
					log.info("Beer brewing process has successfully been started!")
				}
			}
		)
	}

	Order allIngredients() {
		return new Order(items: IngredientType.values())
	}

	RestTemplate restTemplate() {
		return new ExceptionLoggingRestTemplate()
	}

}
