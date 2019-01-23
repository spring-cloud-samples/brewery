package io.spring.cloud.samples.brewery.acceptance;

import java.util.Random;

import io.spring.cloud.samples.brewery.acceptance.common.AbstractBreweryAcceptance;
import io.spring.cloud.samples.brewery.acceptance.common.SpanUtil;
import io.spring.cloud.samples.brewery.acceptance.common.tech.TestConditions;
import io.spring.cloud.samples.brewery.acceptance.model.CommunicationType;
import org.junit.Before;
import org.junit.Test;

import org.springframework.http.RequestEntity;

public class SleuthBreweryAcceptanceTests extends AbstractBreweryAcceptance {
	@Before
	public void before() {
		TestConditions.assumeSleuth();
	}

	@Test
	public void should_successfully_pass_Trace_Id_via_rest_template() {
		// setup:
		warm_up_the_environment(() -> check_brewery(CommunicationType.REST_TEMPLATE));
		// given:
		check_brewery(CommunicationType.REST_TEMPLATE);
	}

	private void check_brewery(CommunicationType communicationType) {
		String referenceProcessId = SpanUtil.idToHex(new Random().nextLong());
		RequestEntity requestEntity = an_order_for_all_ingredients_with_process_id(referenceProcessId, communicationType);
		// when:
		presenting_service_has_been_called(requestEntity);
		// and:
		requestEntity = an_order_for_all_ingredients_with_process_id(referenceProcessId, communicationType);
		presenting_service_has_been_called(requestEntity);
		// then:
		beer_has_been_brewed_for_process_id(referenceProcessId);
		// and:
		entry_for_trace_id_is_present_in_Zipkin(referenceProcessId);
		// and:
		dependency_graph_is_correct();
	}

	@Test
	public void should_successfully_pass_Trace_Id_via_feign() {
		// setup:
		warm_up_the_environment(() -> check_brewery(CommunicationType.FEIGN));
		// given:
		check_brewery(CommunicationType.FEIGN);
	}

}
