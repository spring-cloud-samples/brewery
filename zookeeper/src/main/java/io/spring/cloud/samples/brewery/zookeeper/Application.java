package io.spring.cloud.samples.brewery.zookeeper;

import org.apache.curator.test.TestingServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
public class Application {

    @Bean TestingServer testingServer() throws Exception {
        return new TestingServer(2181);
    }

    public static void main(String[] args) {
        new SpringApplication(Application.class).run(args);
    }
}
