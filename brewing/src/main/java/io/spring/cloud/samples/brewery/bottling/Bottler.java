package io.spring.cloud.samples.brewery.bottling;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import io.spring.cloud.samples.brewery.common.BottlingService;
import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import io.spring.cloud.samples.brewery.common.model.Wort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.TraceManager;
import org.springframework.cloud.sleuth.instrument.hystrix.TraceCommand;
import org.springframework.cloud.sleuth.trace.TraceContextHolder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
class Bottler implements BottlingService {

    private final BottlerService bottlerService;
    private final TraceManager traceManager;

    @Autowired
    public Bottler(BottlerService bottlerService, TraceManager traceManager) {
        this.bottlerService = bottlerService;
        this.traceManager = traceManager;
    }

	/**
     * [SLEUTH] TraceCommand
     */
    @Override
    public void bottle(Wort wort, String processId, String testCommunicationType) {
        log.info("Current span is [{}]", TraceContextHolder.isTracing() ? TraceContextHolder.getCurrentSpan() : "");
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
                bottlerService.bottle(wort, processId);
                return null;
            }
        }.execute();
    }
}
