package io.spring.cloud.samples.brewery.acceptance;

import io.spring.cloud.samples.brewery.acceptance.common.AbstractBreweryAcceptance;
import io.spring.cloud.samples.brewery.acceptance.common.tech.TestConditions;
import io.spring.cloud.samples.brewery.acceptance.model.CommunicationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class SleuthBreweryAcceptanceTests extends AbstractBreweryAcceptance {
	@BeforeEach
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

	@Test
	public void should_successfully_pass_Trace_Id_via_feign() {
		// setup:
		warm_up_the_environment(() -> check_brewery(CommunicationType.FEIGN));
		// given:
		check_brewery(CommunicationType.FEIGN);
	}

}
