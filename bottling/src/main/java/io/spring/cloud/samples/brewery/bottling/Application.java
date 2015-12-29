package io.spring.cloud.samples.brewery.bottling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.sleuth.Sampler;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

import io.spring.cloud.samples.brewery.common.events.EventSource;

@SpringBootApplication
@EnableAsync
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableDiscoveryClient
@EnableFeignClients
@EnableHystrix
@EnableBinding(EventSource.class)
public class Application {

    @Bean
    Sampler sampler() {
        return new AlwaysSampler();
    }

    public static void main(String[] args) {
        new SpringApplication(Application.class).run(args);
    }
}
