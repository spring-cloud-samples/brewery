package io.spring.cloud.samples.brewery.presenting.present
import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import io.spring.cloud.samples.brewery.common.TestConfigurationHolder
import io.spring.cloud.samples.brewery.presenting.config.Versions
import io.spring.cloud.samples.brewery.presenting.feed.FeedRepository
import io.spring.cloud.samples.brewery.presenting.feed.ProcessState
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.cloud.sleuth.Trace
import org.springframework.cloud.sleuth.TraceManager
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate

import static io.spring.cloud.samples.brewery.common.TestRequestEntityBuilder.requestEntity
import static io.spring.cloud.samples.brewery.common.TestConfigurationHolder.TestCommunicationType.FEIGN
import static org.springframework.web.bind.annotation.RequestMethod.GET
import static org.springframework.web.bind.annotation.RequestMethod.POST

@Slf4j
@RestController
@RequestMapping('/present')
@TypeChecked
class PresentController {

    private final FeedRepository feedRepository
    private final TraceManager traceManager
    private final AggregationServiceClient aggregationServiceClient
    private final RestTemplate restTemplate

    @Autowired
    public PresentController(FeedRepository feedRepository, TraceManager traceManager,
                             AggregationServiceClient aggregationServiceClient, @LoadBalanced RestTemplate restTemplate) {
        this.feedRepository = feedRepository
        this.traceManager = traceManager
        this.aggregationServiceClient = aggregationServiceClient
        this.restTemplate = restTemplate
    }

    @RequestMapping(
            value = "/order",
            method = POST)
    String order(HttpEntity<String> body) {
        String processId = body.headers.containsKey('PROCESS-ID') ? body.headers.getFirst('PROCESS-ID') : null
        log.info("Making new order with [$body.body]")
        Trace trace = this.traceManager.startSpan("calling_aggregation")
        String result;
        switch (TestConfigurationHolder.TEST_CONFIG.get().getTestCommunicationType()) {
            case FEIGN:
                result = useFeignToCallAggregation(body, trace, processId);
                break;
            default:
                result = useRestTemplateToCallAggregation(body, trace, processId)
        }
        traceManager.close(trace)
        return result
    }

    private String useRestTemplateToCallAggregation(HttpEntity<String> body, Trace trace, String processId) {
        return restTemplate.exchange(requestEntity()
                .processId(trace.getSpan().getTraceId())
                .contentTypeVersion(Versions.AGGREGATING_CONTENT_TYPE_V1)
                .serviceName("aggregating")
                .url("ingredients")
                .httpMethod(HttpMethod.POST)
                .processId(processId)
                .body(body.body)
                .build(), String.class).body;
    }

    private String useFeignToCallAggregation(HttpEntity<String> body, Trace trace, String processId) {
        return aggregationServiceClient.getIngredients(body.body,
                processId != null ? processId : trace.getSpan().getTraceId(),
                TestConfigurationHolder.TEST_CONFIG.get().getTestCommunicationType().name())
    }

    @RequestMapping(value = "/maturing", method = GET)
    String maturing() {
        return feedRepository.countFor(ProcessState.MATURING)
    }

    @RequestMapping(value = "/bottling", method = GET)
    String bottling() {
        return feedRepository.countFor(ProcessState.BOTTLING)
    }

    @RequestMapping(value = "/bottles", method = GET)
    String bottles() {
        return feedRepository.getBottles()
    }
}
