package io.spring.cloud.samples.brewery.maturing;

import io.spring.cloud.samples.brewery.common.MaturingService;
import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import org.slf4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
class Maturer implements MaturingService {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(Maturer.class);
    private final BottlingServiceUpdater bottlingServiceUpdater;

    @Autowired
    public Maturer(BottlingServiceUpdater bottlingServiceUpdater) {
        this.bottlingServiceUpdater = bottlingServiceUpdater;
    }

    @Override
    public void distributeIngredients(io.spring.cloud.samples.brewery.common.model.Ingredients ingredients, String processId, String testCommunicationType) {
        log.info("I'm in the maturing service. Will distribute ingredients");
        TestConfigurationHolder configurationHolder = TestConfigurationHolder.builder().testCommunicationType(TestConfigurationHolder.TestCommunicationType.valueOf(testCommunicationType)).build();
        bottlingServiceUpdater.updateBottlingServiceAboutBrewedBeer(ingredients, processId, configurationHolder);
    }
}
