package io.spring.cloud.samples.brewery.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import io.spring.cloud.samples.brewery.common.TestConfiguration;

@SpringBootApplication
@Import(TestConfiguration.class)
public class Application {

    public static void main(String[] args) {
        new SpringApplication(Application.class).run(args);
    }
}
