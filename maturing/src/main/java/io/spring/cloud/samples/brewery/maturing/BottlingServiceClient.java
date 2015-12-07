package io.spring.cloud.samples.brewery.maturing;

import io.spring.cloud.samples.brewery.maturing.model.Version;
import io.spring.cloud.samples.brewery.maturing.model.Wort;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static io.spring.cloud.samples.brewery.common.TestConfigurationHolder.TEST_COMMUNICATION_TYPE_HEADER_NAME;

@FeignClient("bottling")
@RequestMapping(value = "/bottle", consumes = Version.BOTTLING_V1,
        produces = MediaType.APPLICATION_JSON_VALUE)
public interface BottlingServiceClient {
    @RequestMapping(method = RequestMethod.POST,
            produces = Version.BOTTLING_V1, consumes = Version.BOTTLING_V1)
    void bottle(Wort wort,
                @RequestHeader("PROCESS-ID") String processId,
                @RequestHeader(TEST_COMMUNICATION_TYPE_HEADER_NAME) String testCommunicationType);
}
