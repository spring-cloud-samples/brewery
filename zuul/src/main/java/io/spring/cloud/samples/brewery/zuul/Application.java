package io.spring.cloud.samples.brewery.zuul;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Configuration;

import io.spring.cloud.samples.brewery.common.events.EventSource;

@SpringBootApplication
@Configuration
@EnableZuulProxy
@EnableDiscoveryClient
@EnableBinding(EventSource.class)
public class Application {
    public static void main(String[] args) {
        new SpringApplication(Application.class).run(args);
    }
}
