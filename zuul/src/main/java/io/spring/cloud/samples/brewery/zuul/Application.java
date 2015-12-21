package io.spring.cloud.samples.brewery.zuul;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
@EnableZuulProxy
public class Application {
    public static void main(String[] args) {
        new SpringApplication(Application.class).run(args);
    }
}
