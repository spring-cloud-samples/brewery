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
package io.spring.cloud.samples.brewery.acceptance

import groovy.json.JsonSlurper
import io.spring.cloud.samples.brewery.acceptance.model.CommunicationType
import io.spring.cloud.samples.brewery.acceptance.model.IngredientType
import io.spring.cloud.samples.brewery.acceptance.model.Order
import io.spring.cloud.samples.brewery.acceptance.model.ProcessState
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.boot.test.TestRestTemplate
import org.springframework.http.*
import org.springframework.test.context.ContextConfiguration
import org.springframework.util.JdkIdGenerator
import org.springframework.web.client.RestTemplate
import spock.lang.Specification
import spock.lang.Unroll

import static com.jayway.awaitility.Awaitility.await
import static java.util.concurrent.TimeUnit.SECONDS

@ContextConfiguration(classes = TestConfiguration, loader = SpringApplicationContextLoader)
class BreweryAcceptanceSpec extends Specification {

	// TODO: Run tests from a container so that internal Docker network is accessible
	// @Autowired @LoadBalanced RestTemplate loadBalanced
	RestTemplate restTemplate = new TestRestTemplate()
	@Value('${presenting.url:http://localhost:9091}') String presentingUrl
	@Value('${presenting.timeout:30}') Integer timeout

	@Unroll
	def 'should successfully brew the beer via [#communicationType] and processId [#referenceProcessId]'() {
		given:
		    RequestEntity requestEntity = an_order_for_all_ingredients_with_process_id(referenceProcessId, communicationType)
		when: 'the presenting service has been called with all ingredients'
			presenting_service_has_been_called(requestEntity)
		then: 'eventually beer for that process id will be brewed'
			await().atMost(timeout, SECONDS).until(beer_has_been_brewed_for_process_id(referenceProcessId))
		where:
		    // will add FEIGN once REST_TEMPLATE tests stabilize
			communicationType << [CommunicationType.REST_TEMPLATE]
			referenceProcessId = new JdkIdGenerator().generateId().toString()
	}

	private Runnable beer_has_been_brewed_for_process_id(String processId) {
		return new Runnable() {
			@Override
			void run() {
				ResponseEntity<String> process = checkStateOfTheProcess(processId)
				assert process.statusCode == HttpStatus.OK
				assert stateFromJson(process) == ProcessState.DONE.name()
			}
		}
	}

	private String stateFromJson(ResponseEntity<String> process) {
		return new JsonSlurper().parseText(process.body).state.toUpperCase()
	}

	private RequestEntity an_order_for_all_ingredients_with_process_id(String processId, CommunicationType communicationType) {
		HttpHeaders headers = new HttpHeaders()
		headers.add("PROCESS-ID", processId)
		headers.add("TEST-COMMUNICATION-TYPE", communicationType.name())
		// URI uri = URI.create("http://presenting/present/order")
		URI uri = URI.create("${presentingUrl}/present/order")
		return new RequestEntity<>(allIngredients(), headers, HttpMethod.POST, uri)
	}

	private ResponseEntity<String> presenting_service_has_been_called(RequestEntity requestEntity) {
		return restTemplate.exchange(requestEntity, String)
	}

	private Order allIngredients() {
		return new Order(items: IngredientType.values())
	}

	private ResponseEntity<String> checkStateOfTheProcess(String processId) {
		//URI uri = URI.create("http://presenting/feed/process/$processId")
		URI uri = URI.create("${presentingUrl}/feed/process/$processId")
		HttpHeaders headers = new HttpHeaders()
		return restTemplate.exchange(new RequestEntity<>(headers, HttpMethod.GET, uri), String)
	}

}
