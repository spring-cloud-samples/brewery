package io.spring.cloud.samples.brewery;

import java.util.function.Supplier;

import io.spring.cloud.samples.brewery.common.events.Event;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableFeignClients
public class Application {

    public static void main(String[] args) {
        new SpringApplication(Application.class).run(args);
    }

    @Bean
    EmitterProcessor<Event> emitterProcessor() {
        return EmitterProcessor.create();
    }

    @Bean
    Supplier<Flux<Event>> events(EmitterProcessor<Event> processor) {
        return () -> processor;
    }
}
