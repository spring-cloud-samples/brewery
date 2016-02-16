package io.spring.cloud.samples.brewery.presenting.feed

import groovy.transform.TypeChecked
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.sleuth.Span
import org.springframework.cloud.sleuth.Tracer
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

import static io.spring.cloud.samples.brewery.presenting.config.Versions.PRESENTING_JSON_VERSION_1
import static org.springframework.web.bind.annotation.RequestMethod.GET
import static org.springframework.web.bind.annotation.RequestMethod.PUT

@Slf4j
@RestController
@RequestMapping('/feed')
@TypeChecked
class FeedController {

    private final FeedRepository feedRepository
    private final Tracer tracer

    @Autowired
    FeedController(FeedRepository feedRepository, Tracer tracer) {
        this.feedRepository = feedRepository
        this.tracer = tracer
    }

    @RequestMapping(
            value = "/maturing",
            produces = PRESENTING_JSON_VERSION_1,
            consumes = PRESENTING_JSON_VERSION_1,
            method = PUT)
    public String maturing(@RequestHeader("PROCESS-ID") String processId) {
        log.info("new maturing with process [$processId]")
        Span span = tracer.startTrace("local:inside_presenting_maturing_feed")
        try {
            return feedRepository.addModifyProcess(processId, ProcessState.MATURING)
        } finally {
            tracer.close(span);
        }
    }

    @RequestMapping(
            value = "/bottling",
            produces = PRESENTING_JSON_VERSION_1,
            consumes = PRESENTING_JSON_VERSION_1,
            method = PUT)
    public String bottling(@RequestHeader("PROCESS-ID") String processId) {
        log.info("new bottling process [$processId]")
        Span span = tracer.startTrace("local:inside_presenting_bottling_feed")
        try {
            return feedRepository.addModifyProcess(processId, ProcessState.BOTTLING)
        } finally {
            tracer.close(span)
        }
    }

    @RequestMapping(
            value = "/bottles/{bottles}",
            produces = PRESENTING_JSON_VERSION_1,
            consumes = PRESENTING_JSON_VERSION_1,
            method = PUT)
    public String bottles(@PathVariable Integer bottles, @RequestHeader("PROCESS-ID") String processId) {
        log.info("bottles number: ${bottles}")
        return feedRepository.setBottles(processId, bottles)
    }

    @RequestMapping(
            value = "/process/{processId}",
            method = GET)
    public ResponseEntity process(@PathVariable String processId) {
        log.info("query for the process state with processId [$processId]")
        return feedRepository.getProcessStateForId(processId)
    }

    @RequestMapping(
            value = "/process",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = GET)
    public ResponseEntity<Set<Process>> allProcesses() {
        return ResponseEntity.ok(feedRepository.processes)
    }

    @RequestMapping(
            method = GET
    )
    public String show() {
        return feedRepository.showStatuses()
    }
}
