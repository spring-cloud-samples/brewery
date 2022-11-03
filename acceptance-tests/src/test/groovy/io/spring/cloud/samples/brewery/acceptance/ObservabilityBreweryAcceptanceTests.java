package io.spring.cloud.samples.brewery.acceptance;

import io.spring.cloud.samples.brewery.acceptance.common.AbstractBreweryAcceptance;
import io.spring.cloud.samples.brewery.acceptance.common.tech.TestConditions;
import io.spring.cloud.samples.brewery.acceptance.model.CommunicationType;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ObservabilityBreweryAcceptanceTests extends AbstractBreweryAcceptance {

    @Test
    public void should_successfully_pass_Trace_Id_via_rest_template() {
        // given:
        check_brewery(CommunicationType.REST_TEMPLATE);
    }

    @Disabled("TODO: Waiting for new release of OpenFeign")
    @Test
    public void should_successfully_pass_Trace_Id_via_feign() {
        // given:
        check_brewery(CommunicationType.FEIGN);
    }

}
