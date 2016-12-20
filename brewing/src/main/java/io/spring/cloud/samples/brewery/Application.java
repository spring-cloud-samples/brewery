package io.spring.cloud.samples.brewery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.sleuth.util.ExceptionUtils;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

import io.spring.cloud.samples.brewery.common.events.EventSource;

@SpringBootApplication
@EnableAsync
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableDiscoveryClient
@EnableFeignClients
@EnableBinding(EventSource.class)
public class Application {

    public static void main(String[] args) {
        ExceptionUtils.setFail(true);
        new SpringApplication(Application.class).run(args);
    }
}
