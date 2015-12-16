package io.spring.cloud.samples.brewery.maturing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.sleuth.zipkin.stream.EnableZipkinStreamServer;

@SpringBootApplication
@EnableZipkinStreamServer
public class Application {
    public static void main(String[] args) {
        new SpringApplication(Application.class).run(args);
    }
}
