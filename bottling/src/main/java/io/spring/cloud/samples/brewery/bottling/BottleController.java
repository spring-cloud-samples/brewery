package io.spring.cloud.samples.brewery.bottling;

import io.spring.cloud.samples.brewery.bottling.model.BottleRequest;
import io.spring.cloud.samples.brewery.bottling.model.Version;
import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.trace.TraceContextHolder;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import static org.springframework.cloud.sleuth.Trace.TRACE_ID_NAME;

@RestController
@RequestMapping(value = "/bottle", consumes = Version.V1, produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class BottleController {

    private final BottlerService bottlerService;

    @Autowired
    public BottleController(BottlerService bottlerService) {
        this.bottlerService = bottlerService;
    }

    @RequestMapping(method = RequestMethod.POST, produces = Version.V1, consumes = Version.V1)
    public void bottle(@RequestBody BottleRequest bottleRequest,
                       @RequestHeader("PROCESS-ID") String processId) {
        log.info("Current traceid is {}", TraceContextHolder.isTracing() ? TraceContextHolder.getCurrentSpan().getTraceId() : "");
        log.info("Process ID from headers {}", processId);
        bottlerService.bottle(bottleRequest, processId);
    }

}
