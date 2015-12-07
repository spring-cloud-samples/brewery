package io.spring.cloud.samples.brewery.zookeeper;

import com.google.common.collect.Iterables;
import org.apache.curator.test.TestingServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Arrays;

@SpringBootApplication
@Configuration
@Profile("dev")
public class Application {

    @Bean(destroyMethod = "close")
    TestingServer testingServer(@Value("${spring.cloud.zookeeper.connectString:localhost:2181}") String connectString) throws Exception {
        int port = Integer.valueOf(Iterables.getLast(Arrays.asList(connectString.split(":"))));
        return new TestingServer(port);
    }

    public static void main(String[] args) {
        new SpringApplication(Application.class).run(args);
    }
}
