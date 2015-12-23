package io.spring.cloud.samples.brewery.bottling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.TraceManager;
import org.springframework.cloud.sleuth.instrument.hystrix.TraceCommand;
import org.springframework.cloud.sleuth.trace.TraceContextHolder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import io.spring.cloud.samples.brewery.bottling.model.BottleRequest;
import io.spring.cloud.samples.brewery.bottling.model.Version;
import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(value = "/bottle", consumes = Version.V1, produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class BottleController {

    private final BottlerService bottlerService;
    private final TraceManager traceManager;

    @Autowired
    public BottleController(BottlerService bottlerService, TraceManager traceManager) {
        this.bottlerService = bottlerService;
        this.traceManager = traceManager;
    }

	/**
     * [SLEUTH] TraceCommand
     */
    @RequestMapping(method = RequestMethod.POST, produces = Version.V1, consumes = Version.V1)
    public void bottle(@RequestBody BottleRequest bottleRequest,
                       @RequestHeader("PROCESS-ID") String processId) {
        log.info("Current traceid is {}", TraceContextHolder.isTracing() ? TraceContextHolder.getCurrentSpan().getTraceId() : "");
        log.info("Process ID from headers {}", processId);
        String groupKey = "bottling";
        String commandKey = "bottle";
        HystrixCommand.Setter setter = HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey));
        TestConfigurationHolder testConfigurationHolder = TestConfigurationHolder.TEST_CONFIG.get();
        new TraceCommand<Void>(traceManager, setter) {

            @Override
            public Void doRun() throws Exception {
                TestConfigurationHolder.TEST_CONFIG.set(testConfigurationHolder);
                log.info("Sending info to bottling service about process id [{}]", processId);
                bottlerService.bottle(bottleRequest, processId);
                return null;
            }
        }.execute();
    }

}
