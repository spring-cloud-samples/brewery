package io.spring.cloud.samples.brewery.ingredients;

import io.spring.cloud.samples.brewery.common.TestConfiguration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(TestConfiguration.class)
public class IngredientsApplication {

    public static void main(String[] args) {
        new SpringApplication(IngredientsApplication.class).run(args);
    }
}
