package io.spring.cloud.samples.brewery.bottling;

import io.spring.cloud.samples.brewery.common.BottlingService;
import io.spring.cloud.samples.brewery.common.model.Wort;
import org.slf4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;

@Service
class Bottler implements BottlingService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(Bottler.class);
    private final BottlerService bottlerService;
    private final CircuitBreakerFactory circuitBreakerFactory;

    @Autowired
    public Bottler(BottlerService bottlerService, CircuitBreakerFactory circuitBreakerFactory) {
        this.bottlerService = bottlerService;
        this.circuitBreakerFactory = circuitBreakerFactory;
    }

	/**
     * [OBSERVABILITY] TraceCommand
     */
    @Override
    public void bottle(Wort wort, String processId, String testCommunicationType) {
        log.info("I'm in the bottling service");
        log.info("Process ID from headers {}", processId);
        circuitBreakerFactory.create("bottle").run(() -> {
            log.info("Sending info to bottling service about process id [{}]", processId);
            bottlerService.bottle(wort, processId);
            return null;
        });
    }
}
