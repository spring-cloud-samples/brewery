package io.spring.cloud.samples.brewery.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import brave.sampler.Sampler;

/**
 * @author Olga Maciaszek-Sharma
 */
@Configuration
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean Sampler defaultSampler() {
		return Sampler.ALWAYS_SAMPLE;
	}
}
