package io.spring.cloud.samples.brewery.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
@EnableConfigServer
public class Application {
    public static void main(String[] args) {
        new SpringApplication(Application.class).run(args);
    }
}
