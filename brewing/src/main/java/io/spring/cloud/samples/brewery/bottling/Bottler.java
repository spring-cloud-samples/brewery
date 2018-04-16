package io.spring.cloud.samples.brewery.bottling;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;

import org.springframework.beans.factory.annotation.Autowired;
import brave.Tracer;
import org.springframework.cloud.sleuth.instrument.hystrix.TraceCommand;
import org.springframework.stereotype.Service;

import io.spring.cloud.samples.brewery.common.BottlingService;
import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import io.spring.cloud.samples.brewery.common.model.Wort;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
class Bottler implements BottlingService {

    private final BottlerService bottlerService;
    private final Tracer tracer;

    @Autowired
    public Bottler(BottlerService bottlerService, Tracer tracer) {
        this.bottlerService = bottlerService;
        this.tracer = tracer;
    }

	/**
     * [SLEUTH] TraceCommand
     */
    @Override
    public void bottle(Wort wort, String processId, String testCommunicationType) {
        log.info("I'm in the bottling service");
        log.info("Process ID from headers {}", processId);
        String groupKey = "bottling";
        String commandKey = "bottle";
        HystrixCommand.Setter setter = HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey));
        TestConfigurationHolder testConfigurationHolder = TestConfigurationHolder.TEST_CONFIG.get();
        new TraceCommand<Void>(tracer, setter) {
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
