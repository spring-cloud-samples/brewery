package io.spring.cloud.samples.brewery.presenting.feed;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static io.spring.cloud.samples.brewery.presenting.config.Versions.PRESENTING_JSON_VERSION_1;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

/**
 * @author Marcin Grzejszczak
 */
@RestController
@RequestMapping("/feed")
class FeedController {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(FeedController.class);
    private final FeedRepository feedRepository;
    private final ObservationRegistry observationRegistry;

    @Autowired
    FeedController(FeedRepository feedRepository, ObservationRegistry observationRegistry) {
        this.feedRepository = feedRepository;
        this.observationRegistry = observationRegistry;
    }

    @RequestMapping(
            value = "/maturing",
            produces = PRESENTING_JSON_VERSION_1,
            consumes = PRESENTING_JSON_VERSION_1,
            method = PUT)
    public void maturing(@RequestHeader("PROCESS-ID") String processId) {
        log.info("new maturing with process [{}]", processId);
        Observation.createNotStarted("inside_presenting_maturing_feed", observationRegistry)
                .observe(() -> feedRepository.addModifyProcess(processId, ProcessState.MATURING));
    }

    @RequestMapping(
            value = "/bottling",
            produces = PRESENTING_JSON_VERSION_1,
            consumes = PRESENTING_JSON_VERSION_1,
            method = PUT)
    public void bottling(@RequestHeader("PROCESS-ID") String processId) {
        log.info("new bottling process [{}]", processId);
        Observation.createNotStarted("inside_presenting_maturing_feed", observationRegistry)
                .observe(() -> feedRepository.addModifyProcess(processId, ProcessState.BOTTLING));
    }

    @RequestMapping(
            value = "/bottles/{bottles}",
            produces = PRESENTING_JSON_VERSION_1,
            consumes = PRESENTING_JSON_VERSION_1,
            method = PUT)
    public void bottles(@PathVariable Integer bottles, @RequestHeader("PROCESS-ID") String processId) {
        log.info("bottles number: {}", bottles);
        feedRepository.setBottles(processId, bottles);
    }

    @RequestMapping(
            value = "/process/{processId}",
            method = GET)
    public ResponseEntity process(@PathVariable String processId) {
        log.info("query for the process state with processId [{}]", processId);
        return feedRepository.getProcessStateForId(processId);
    }

    @RequestMapping(
            value = "/process",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = GET)
    public ResponseEntity<Set<Process>> allProcesses() {
        return ResponseEntity.ok(feedRepository.getProcesses());
    }

    @RequestMapping(
            method = GET
    )
    public String show() {
        return feedRepository.showStatuses();
    }
}
