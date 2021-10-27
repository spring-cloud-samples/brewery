package io.spring.cloud.samples.brewery.maturing;

import io.spring.cloud.samples.brewery.common.model.Version;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@FeignClient(name = Collaborators.PRESENTING, path = "/feed")
interface PresentingServiceClient {
    @RequestMapping(
            value = "/maturing",
            produces = Version.PRESENTING_V1,
            consumes = Version.PRESENTING_V1,
            method = PUT)
    String maturingFeed(@RequestHeader("PROCESS-ID") String processId,
                        @RequestHeader("TEST-COMMUNICATION-TYPE") String testCommunicationType);
}
