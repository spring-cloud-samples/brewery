package io.spring.cloud.samples.brewery.bottling;

import io.spring.cloud.samples.brewery.common.model.Version;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import static io.spring.cloud.samples.brewery.common.TestConfigurationHolder.TEST_COMMUNICATION_TYPE_HEADER_NAME;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@FeignClient(Collaborators.PRESENTING)
@RequestMapping("/feed")
interface PresentingClient {
    @RequestMapping(
            value = "/bottles/{bottles}",
            produces = Version.PRESENTING_V1,
            consumes = Version.PRESENTING_V1,
            method = PUT)
    String updateBottles(@PathVariable("bottles") int bottles,
                         @RequestHeader("PROCESS-ID") String processId,
                         @RequestHeader(TEST_COMMUNICATION_TYPE_HEADER_NAME) String testCommunicationType);

    @RequestMapping(
            value = "/bottling",
            produces = Version.PRESENTING_V1,
            consumes = Version.PRESENTING_V1,
            method = PUT)
    void bottlingFeed(@RequestHeader("PROCESS-ID") String processId,
                      @RequestHeader(TEST_COMMUNICATION_TYPE_HEADER_NAME) String testCommunicationType);
}
