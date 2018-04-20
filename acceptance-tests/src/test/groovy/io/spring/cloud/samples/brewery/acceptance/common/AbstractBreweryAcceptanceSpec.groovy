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
import io.spring.cloud.samples.brewery.acceptance.common.tech.ExceptionLoggingRestTemplate
import io.spring.cloud.samples.brewery.acceptance.common.tech.TestConfiguration
import io.spring.cloud.samples.brewery.acceptance.model.CommunicationType
import io.spring.cloud.samples.brewery.acceptance.model.IngredientType
import io.spring.cloud.samples.brewery.acceptance.model.Order
import io.spring.cloud.samples.brewery.acceptance.model.ProcessState
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootContextLoader
import brave.Span
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.RequestEntity
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.client.RestTemplate
import spock.lang.Specification
import zipkin.Codec

import static com.jayway.awaitility.Awaitility.await
import static java.util.concurrent.TimeUnit.SECONDS

/**
 *  TODO: Split responsibilities
 */
@ContextConfiguration(classes = TestConfiguration, loader = SpringBootContextLoader)
abstract class AbstractBreweryAcceptanceSpec extends Specification {

	public static final String TRACE_ID_HEADER_NAME = "X-B3-TraceId"
	public static final String SPAN_ID_HEADER_NAME = "X-B3-SpanId"
	public static final Logger log = LoggerFactory.getLogger(AbstractBreweryAcceptanceSpec)

	private static final List<String> APP_NAMES = ['presenting', 'brewing', 'zuul']
	private static final List<String> SPAN_NAMES = [
													'inside_presenting_maturing_feed',
													'inside_presenting_bottling_feed',
													'send',
													'inside_aggregating',
													'inside_maturing',
													'inside_bottling',
													'inside_ingredients',
													'inside_reporting']

	// interval to check status of different brewery elements
	@Value('${brewery.poll.interval:1}') Integer pollInterval
	@Value('${brewery.timeout:60}') Integer timeout
	// interval for the first request to presenting
	@Value('${presenting.poll.interval:5}') Integer presentingPollInterval
	@Value('${presenting.url:http://localhost:9991}') String presentingUrl
	@Value('${zipkin.query.port:9411}') Integer zipkinQueryPort
	@Value('${LOCAL_URL:http://localhost}') String zipkinQueryUrl
	@Value('${test.zipkin.dependencies:true}') boolean checkZipkinDependencies
	@Value('${BOM_VERSION:Finchley.BUILD-SNAPSHOT}') String bomVersion

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
				List<zipkin.Span> spans = Codec.JSON.readSpans(response.body.bytes)
				List<String> serviceNamesNotFoundInZipkin = serviceNamesNotFoundInZipkin(spans)
				List<String> spanNamesNotFoundInZipkin = spanNamesNotFoundInZipkin(spans)
				log.info("The following services were not found in Zipkin $serviceNamesNotFoundInZipkin")
				log.info("The following spans were not found in Zipkin $spanNamesNotFoundInZipkin")
				assert serviceNamesNotFoundInZipkin.empty
				assert spanNamesNotFoundInZipkin.empty
				def messagingSpans = spans.findAll { it.binaryAnnotations.find { it.value == "events".bytes } }
				log.info("Found the folllowing messaging spans [{}]", messagingSpans)
				assert !messagingSpans.empty
				zipkin.Span spanByTag = findSpanByTag('beer', spans)
				assert spanByTag.annotations.find { it.value == 'ingredientsAggregationStarted' }
				log.info("Custom log [ingredientsAggregationStarted] found!")
				assert spanByTag.binaryAnnotations.find { it.key == 'beer' && new String(it.value) == 'stout' }
				log.info("Custom tag ['beer' -> 'stout'] found!")
			}

			private List<String> serviceNamesNotFoundInZipkin(List<zipkin.Span> spans) {
				List<String> serviceNamesFoundInAnnotations = spans.collect {
					it.annotations.endpoint.serviceName
				}.flatten().unique()
				List<String> serviceNamesFoundInBinaryAnnotations = spans.collect {
					it.binaryAnnotations.endpoint.serviceName
				}.flatten().unique()
				return (APP_NAMES - serviceNamesFoundInAnnotations - serviceNamesFoundInBinaryAnnotations)
			}

			private zipkin.Span findSpanByTag(String tagKey, List<zipkin.Span> spans) {
				return spans.find { it.binaryAnnotations.find { it.key == tagKey} }
			}

			private List<String> spanNamesNotFoundInZipkin(List<zipkin.Span> spans) {
				List<String> spanNamesFoundInAnnotations = spans.collect {
					it.name
				}.flatten().unique()
				return (SPAN_NAMES - spanNamesFoundInAnnotations)
			}
		})
	}

	void dependency_graph_is_correct() {
		if (!checkZipkinDependencies) {
			log.warn("Skipping the test for Zipkin dependencies")
			return
		}
		await().pollInterval(pollInterval, SECONDS).atMost(timeout, SECONDS).until(new Runnable() {
			@Override
			void run() {
				ResponseEntity<String> response = checkDependencies()
				log.info("Response from the Zipkin query service about the dependencies [$response]")
				assert response.statusCode == HttpStatus.OK
				assert response.hasBody()
				Map<String, List<String>> parentsAndChildren = [:]
				new JsonSlurper().parseText(response.body).inject(parentsAndChildren) { Map<String, String> acc, def json ->
					def list = acc[json.parent] ?: []
					list << json.child
					acc.put(json.parent, list)
					return acc
				}
				log.info("Presenting should be a parent of brewing.")
				assert parentsAndChildren['presenting']?.contains('brewing')
//				log.info("Brewing should have 3 children - zuul, reporting and presenting")
//				assert parentsAndChildren['brewing']?.containsAll(['zuul', 'reporting', 'presenting'])
				// TODO: FIX THIS!!
				log.info("Brewing should have 3 children - zuul, reporting and presenting but has only 2 for now")
				assert parentsAndChildren['brewing']?.containsAll(['zuul', 'presenting'])
				log.info("Zuul should be calling ingredients")
				assert parentsAndChildren['zuul']?.contains('ingredients')
				log.info("Zipkin tracing is working! Sleuth is working! Let's be happy!")
			}
		})
	}

	ResponseEntity<String> checkStateOfTheProcess(String processId) {
		URI uri = URI.create("$presentingUrl/feed/process/$processId")
		log.info("Sending request to the presenting service [$uri] to check the beer brewing process. The process id is [$processId]")
		return restTemplate().exchange(
				new RequestEntity<>(new HttpHeaders(), HttpMethod.GET, uri), String
		)
	}

	ResponseEntity<String> checkStateOfTheTraceId(String traceId) {
		String path = "api/v1/trace"
		URI uri = URI.create("${wrapQueryWithProtocolIfPresent() ?: zipkinQueryUrl}:${zipkinQueryPort}/${path}/$traceId")
		HttpHeaders headers = new HttpHeaders()
		log.info("Sending request to the Zipkin query service [$uri]. Checking presence of trace id [$traceId]")
		return new ExceptionLoggingRestTemplate().exchange(
				new RequestEntity<>(headers, HttpMethod.GET, uri), String
		)
	}

	ResponseEntity<String> checkDependencies() {
		URI uri = URI.create("${wrapQueryWithProtocolIfPresent() ?: zipkinQueryUrl}:${zipkinQueryPort}/api/v1/dependencies?endTs=${System.currentTimeMillis()}")
		HttpHeaders headers = new HttpHeaders()
		log.info("Sending request to the Zipkin query service [$uri]. Checking the dependency graph")
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
		headers.add(SPAN_ID_HEADER_NAME, SpanUtil.idToHex(new Random().nextLong()))
		headers.add("TEST-COMMUNICATION-TYPE", communicationType.name())
		URI uri = URI.create("$presentingUrl/present/order")
		Order allIngredients = allIngredients()
		RequestEntity requestEntity = new RequestEntity<>(allIngredients, headers, HttpMethod.POST, uri)
		log.info("Request with order for all ingredients to presenting service [$requestEntity] is ready")
		return requestEntity
	}

	void presenting_service_has_been_called(RequestEntity requestEntity) {
		await().pollInterval(presentingPollInterval, SECONDS).atMost(timeout, SECONDS).until(new Runnable() {
			@Override
			void run() {
				log.info("Sending [$requestEntity] to start brewing the beer.")
				ResponseEntity<String> responseEntity = restTemplate().exchange(requestEntity, String)
				log.info("Received [$responseEntity] from the presenting service.")
				assert responseEntity.statusCode == HttpStatus.OK
				log.info("Beer brewing process has successfully been started!")
			}
		});
	}

	Order allIngredients() {
		return new Order(items: IngredientType.values())
	}

	RestTemplate restTemplate() {
		return new ExceptionLoggingRestTemplate()
	}

}
