package io.spring.cloud.samples.brewery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.sleuth.util.ExceptionUtils;

@SpringBootApplication
@EnableDiscoveryClient
public class Application {

    public static void main(String[] args) {
        ExceptionUtils.setFail(true);
        new SpringApplication(Application.class).run(args);
    }
}
