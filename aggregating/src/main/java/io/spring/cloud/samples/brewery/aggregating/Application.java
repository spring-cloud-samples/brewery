package io.spring.cloud.samples.brewery.aggregating;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableDiscoveryClient
@EnableFeignClients
public class Application {

    public static void main(String[] args) {
        new SpringApplication(Application.class).run(args);
    }
}
