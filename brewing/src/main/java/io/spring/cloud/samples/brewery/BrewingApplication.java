package io.spring.cloud.samples.brewery;

import io.spring.cloud.samples.brewery.common.TestConfiguration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableFeignClients
@Import(TestConfiguration.class)
public class BrewingApplication {

	public static void main(String[] args) {
		new SpringApplication(BrewingApplication.class).run(args);
	}
}
