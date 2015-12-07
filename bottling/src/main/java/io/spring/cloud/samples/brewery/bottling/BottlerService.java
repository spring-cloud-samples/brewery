package io.spring.cloud.samples.brewery.bottling;

import io.spring.cloud.samples.brewery.bottling.model.BottleRequest;
import io.spring.cloud.samples.brewery.bottling.model.Version;
import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static io.spring.cloud.samples.brewery.common.TestConfigurationHolder.TEST_CONFIG;
import static io.spring.cloud.samples.brewery.common.TestConfigurationHolder.TestCommunicationType.FEIGN;
import static io.spring.cloud.samples.brewery.common.TestRequestEntityBuilder.requestEntity;

@Slf4j
class BottlerService {

    private BottlingWorker bottlingWorker;
    private PresentingClient presentingClient;
    private RestTemplate restTemplate;

    public BottlerService(BottlingWorker bottlingWorker, PresentingClient presentingClient,
                          RestTemplate restTemplate) {
        this.bottlingWorker = bottlingWorker;
        this.presentingClient = presentingClient;
        this.restTemplate = restTemplate;
    }

    void bottle(BottleRequest bottleRequest, String processId) {
        notifyPrezentatr(processId);
        bottlingWorker.bottleBeer(bottleRequest.getWort(), processId, TEST_CONFIG.get());
    }

    void notifyPrezentatr(String processId) {
        switch (TEST_CONFIG.get().getTestCommunicationType()) {
            case FEIGN:
                callPresentingViaFeign(processId);
                break;
            default:
                useRestTemplateToCallPresenting(processId);
        }
    }

    private void callPresentingViaFeign(String processId) {
        presentingClient.bottlingFeed(processId, FEIGN.name());
    }

    private void useRestTemplateToCallPresenting(String processId) {
        log.info("Notifying presenting about beer. Process id [{}]", processId);
        restTemplate.exchange(requestEntity()
                .processId(processId)
                .contentTypeVersion(Version.PRESENTING_V1)
                .serviceName("presenting")
                .url("feed/bottling")
                .httpMethod(HttpMethod.PUT)
                .build(), String.class);
    }
}
