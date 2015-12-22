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
import io.spring.cloud.samples.brewery.acceptance.common.AbstractBreweryAcceptanceSpec
import io.spring.cloud.samples.brewery.acceptance.common.tech.TestConditions
import io.spring.cloud.samples.brewery.acceptance.model.CommunicationType
import org.springframework.http.RequestEntity
import org.springframework.util.JdkIdGenerator
import spock.lang.Requires
import spock.lang.Unroll

@Requires({ TestConditions.SLEUTH() })
class SleuthBreweryAcceptanceSpec extends AbstractBreweryAcceptanceSpec {

	@Unroll
	def 'should successfully pass Trace Id via [#communicationType] and processId [#referenceProcessId]'() {
		given:
		    RequestEntity requestEntity = an_order_for_all_ingredients_with_process_id(referenceProcessId, communicationType)
		when: 'the presenting service has been called with all ingredients'
			presenting_service_has_been_called(requestEntity)
		then: 'eventually beer will be brewed with same Trace-Id as the first request'
			beer_has_been_brewed_for_process_id(referenceProcessId)
		and: 'entry will be present in Zipkin'
			entry_for_trace_id_is_present_in_Zipkin(referenceProcessId)
		where:
			communicationType << [CommunicationType.REST_TEMPLATE, CommunicationType.FEIGN]
			referenceProcessId = new JdkIdGenerator().generateId().toString()
	}

}
