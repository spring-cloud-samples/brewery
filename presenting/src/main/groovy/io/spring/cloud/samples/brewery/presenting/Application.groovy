package io.spring.cloud.samples.brewery.presenting

import io.spring.cloud.samples.brewery.common.TestConfiguration
import io.spring.cloud.samples.brewery.common.events.EventSource
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.cloud.netflix.feign.EnableFeignClients
import org.springframework.cloud.sleuth.Sampler
import org.springframework.cloud.sleuth.sampler.AlwaysSampler
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.web.client.RestTemplate

@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableAsync
@EnableDiscoveryClient
@EnableFeignClients
@Import(TestConfiguration.class)
@EnableBinding(EventSource.class)
class Application {

    @Bean Sampler sampler() {
        return new AlwaysSampler();
    }

    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }

    static void main(String[] args) {
        new SpringApplication(Application.class).run(args)
    }
}
