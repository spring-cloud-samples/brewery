package io.spring.cloud.samples.brewery.eureka;

import io.spring.cloud.samples.brewery.common.TestConfigurationHolder;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import io.spring.cloud.samples.brewery.eureka.model.Ingredients;
import io.spring.cloud.samples.brewery.eureka.model.Version;

@FeignClient("maturing")
@RequestMapping(value = "/brew", consumes = Version.MATURING_V1)
public interface MaturingServiceClient {
    @RequestMapping(method = RequestMethod.POST)
    void distributeIngredients(Ingredients ingredients,
                               @RequestHeader("PROCESS-ID") String processId,
                               @RequestHeader(TestConfigurationHolder.TEST_COMMUNICATION_TYPE_HEADER_NAME)
                               String testCommunicationType);


}
