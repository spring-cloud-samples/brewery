package io.spring.cloud.samples.brewery.reporting;

import io.spring.cloud.samples.brewery.common.TestConfiguration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(TestConfiguration.class)
public class ReportingApplication {

    public static void main(String[] args) {
        new SpringApplication(ReportingApplication.class).run(args);
    }
}
