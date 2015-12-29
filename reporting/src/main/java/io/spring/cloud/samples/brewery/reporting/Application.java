package io.spring.cloud.samples.brewery.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Configuration;

import io.spring.cloud.samples.brewery.common.events.EventSink;

@SpringBootApplication
@Configuration
@EnableDiscoveryClient
@EnableBinding(EventSink.class)
public class Application {

    public static void main(String[] args) {
        new SpringApplication(Application.class).run(args);
    }
}
