package io.spring.cloud.samples.brewery.gateway;

import io.spring.cloud.samples.brewery.common.TestConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * @author Olga Maciaszek-Sharma
 */
@SpringBootApplication
@Import(TestConfiguration.class)
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
