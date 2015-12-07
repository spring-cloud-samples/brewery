package io.spring.cloud.samples.brewery.maturing;

import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import io.spring.cloud.samples.brewery.maturing.model.Ingredients;
import io.spring.cloud.samples.brewery.maturing.model.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/brew", consumes = Version.MATURING_V1)
public class BrewController {

    private final BottlingServiceUpdater bottlingServiceUpdater;

    @Autowired
    public BrewController(BottlingServiceUpdater bottlingServiceUpdater) {
        this.bottlingServiceUpdater = bottlingServiceUpdater;
    }

    @RequestMapping(method = RequestMethod.POST)
    public void distributeIngredients(@RequestBody Ingredients ingredients,
                                      @RequestHeader("PROCESS-ID") String processId,
                                      @RequestHeader(value = TestConfigurationHolder.TEST_COMMUNICATION_TYPE_HEADER_NAME,
                                              defaultValue = "REST_TEMPLATE", required = false)
                                          TestConfigurationHolder.TestCommunicationType testCommunicationType) {
        TestConfigurationHolder configurationHolder = TestConfigurationHolder.builder().testCommunicationType(testCommunicationType).build();
        bottlingServiceUpdater.updateBottlingServiceAboutBrewedBeer(ingredients, processId, configurationHolder);
    }
}
