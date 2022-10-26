package io.spring.cloud.samples.brewery.presenting.present;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.BaggageManager;
import io.spring.cloud.samples.brewery.common.TestCommunication;
import io.spring.cloud.samples.brewery.presenting.config.Collaborators;
import io.spring.cloud.samples.brewery.presenting.config.Versions;
import io.spring.cloud.samples.brewery.presenting.feed.FeedRepository;
import io.spring.cloud.samples.brewery.presenting.feed.ProcessState;
import org.slf4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.util.JdkIdGenerator;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import static io.spring.cloud.samples.brewery.common.TestRequestEntityBuilder.requestEntity;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * @author Marcin Grzejszczak
 */
@RestController
@RequestMapping("/present")
class PresentController {
	public static final String PROCESS_ID_HEADER_NAME = "PROCESS-ID";
	private static final Logger log = org.slf4j.LoggerFactory.getLogger(PresentController.class);

	private final FeedRepository feedRepository;
	private final ObservationRegistry observationRegistry;
	private final BrewingServiceClient aggregationServiceClient;
	private final RestTemplate restTemplate;

	private final BaggageManager baggageManager;

	@Autowired
	public PresentController(FeedRepository feedRepository, ObservationRegistry observationRegistry,
		BrewingServiceClient aggregationServiceClient, @LoadBalanced RestTemplate restTemplate, BaggageManager baggageManager) {
		this.feedRepository = feedRepository;
		this.observationRegistry = observationRegistry;
		this.aggregationServiceClient = aggregationServiceClient;
		this.restTemplate = restTemplate;
		this.baggageManager = baggageManager;
	}

	@RequestMapping(
		value = "/order",
		method = POST)
	String order(HttpEntity<String> body) {
		String processIdFromHeaders = body.getHeaders().getFirst(PROCESS_ID_HEADER_NAME);
		String processId = StringUtils.hasText(body.getHeaders().getFirst(PROCESS_ID_HEADER_NAME)) ?
			processIdFromHeaders :
			new JdkIdGenerator().generateId().toString();
		log.info("Making new order with [{}] and processid [{}].", body.getBody(), processId);
		return Observation.createNotStarted("inside_presenting", observationRegistry)
			.observe(() -> {
				String testCommunicationType = TestCommunication.fromBaggage(baggageManager);
				log.info("Found the following communication type [{}]", testCommunicationType);
				switch (testCommunicationType) {
				case "FEIGN":
					return useFeignToCallAggregation(body, processId);
				default:
					return useRestTemplateToCallAggregation(body, processId);
				}
				});
	}

	private String useRestTemplateToCallAggregation(HttpEntity<String> body, String processId) {
		return restTemplate.exchange(requestEntity()
			.contentTypeVersion(Versions.BREWING_CONTENT_TYPE_V1)
			.serviceName(Collaborators.BREWING)
			.url("ingredients")
			.httpMethod(HttpMethod.POST)
			.processId(processId)
			.body(body.getBody())
			.build(), String.class).getBody();
	}

	private String useFeignToCallAggregation(HttpEntity<String> body, String processId) {
		String testCommunicationType = TestCommunication.fromBaggage(baggageManager);
		log.info("Found the following communication type [{}]", testCommunicationType);
		return aggregationServiceClient.getIngredients(body.getBody(),
			processId,
			testCommunicationType);
	}

	@RequestMapping(value = "/maturing", method = GET)
	Long maturing() {
		return feedRepository.countFor(ProcessState.MATURING);
	}

	@RequestMapping(value = "/bottling", method = GET)
	Long bottling() {
		return feedRepository.countFor(ProcessState.BOTTLING);
	}

	@RequestMapping(value = "/bottles", method = GET)
	Integer bottles() {
		return feedRepository.getBottles();
	}
}
