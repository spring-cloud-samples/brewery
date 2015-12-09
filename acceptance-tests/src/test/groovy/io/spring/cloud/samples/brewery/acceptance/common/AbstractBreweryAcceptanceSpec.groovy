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
import org.springframework.web.client.RestTemplate
import spock.lang.Specification

@ContextConfiguration(classes = TestConfiguration, loader = SpringApplicationContextLoader)
@WebIntegrationTest(randomPort = true)
abstract class AbstractBreweryAcceptanceSpec extends Specification {

	@Autowired @LoadBalanced RestTemplate restTemplate
	@Value('${presenting.timeout:30}') Integer timeout

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

	String stateFromJson(ResponseEntity<String> process) {
		return new JsonSlurper().parseText(process.body).state.toUpperCase()
	}

	RequestEntity an_order_for_all_ingredients_with_process_id(String processId, CommunicationType communicationType) {
		HttpHeaders headers = new HttpHeaders()
		headers.add("PROCESS-ID", processId)
		headers.add("TEST-COMMUNICATION-TYPE", communicationType.name())
		URI uri = URI.create("http://presenting/present/order")
		return new RequestEntity<>(allIngredients(), headers, HttpMethod.POST, uri)
	}

	ResponseEntity<String> presenting_service_has_been_called(RequestEntity requestEntity) {
		new ExceptionLoggingRetryTemplate(timeout).execute(
				new RetryCallback<ResponseEntity<String>, Exception>() {
					@Override
					ResponseEntity<String> doWithRetry(RetryContext retryContext) throws Exception {
						return restTemplate.exchange(requestEntity, String)
					}
				}
		)
	}

	Order allIngredients() {
		return new Order(items: IngredientType.values())
	}

	ResponseEntity<String> checkStateOfTheProcess(String processId) {
		URI uri = URI.create("http://presenting/feed/process/$processId")
		HttpHeaders headers = new HttpHeaders()
		return restTemplate.exchange(new RequestEntity<>(headers, HttpMethod.GET, uri), String)
	}

}
