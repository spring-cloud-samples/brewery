package io.spring.cloud.samples.brewery.acceptance;

import java.util.Random;

import io.spring.cloud.samples.brewery.acceptance.common.AbstractBreweryAcceptance;
import io.spring.cloud.samples.brewery.acceptance.common.SpanUtil;
import io.spring.cloud.samples.brewery.acceptance.common.tech.TestConditions;
import io.spring.cloud.samples.brewery.acceptance.model.CommunicationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.RequestEntity;

public class ServiceDiscoveryAcceptanceTests extends AbstractBreweryAcceptance {

	@BeforeEach
	public void before() {
		TestConditions.assumeServiceDiscovery();
	}


	@Test
	public void should_successfully_pass_Trace_Id_via_rest_template() {
		// setup:
		warm_up_the_environment(() -> check_brewery(CommunicationType.REST_TEMPLATE));
		// given:
		check_brewery(CommunicationType.REST_TEMPLATE);
	}

	@Test
	public void should_successfully_brew_the_beer_via_rest_template_and_service_discovery() {
		// given:
		String referenceProcessId = SpanUtil.idToHex(new Random().nextLong());
		RequestEntity requestEntity = an_order_for_all_ingredients_with_process_id(referenceProcessId, CommunicationType.REST_TEMPLATE);
		// when:
		presenting_service_has_been_called(requestEntity);
		// then:
		beer_has_been_brewed_for_process_id(referenceProcessId);
	}

	@Test
	public void should_successfully_brew_the_beer_via_feign_and_service_discovery() {
		// given:
		String referenceProcessId = SpanUtil.idToHex(new Random().nextLong());
		RequestEntity requestEntity = an_order_for_all_ingredients_with_process_id(referenceProcessId, CommunicationType.FEIGN);
		// when:
		presenting_service_has_been_called(requestEntity);
		// then:
		beer_has_been_brewed_for_process_id(referenceProcessId);
	}

}
