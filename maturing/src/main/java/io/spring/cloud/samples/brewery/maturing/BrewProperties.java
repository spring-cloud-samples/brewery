package io.spring.cloud.samples.brewery.maturing;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("brew")
@Data
public class BrewProperties {

    private Long timeout = 10L;
}
