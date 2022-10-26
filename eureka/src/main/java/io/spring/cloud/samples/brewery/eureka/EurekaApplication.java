package io.spring.cloud.samples.brewery.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
@EnableEurekaServer
public class EurekaApplication {
    public static void main(String[] args) {
        new SpringApplication(EurekaApplication.class).run(args);
    }
}
