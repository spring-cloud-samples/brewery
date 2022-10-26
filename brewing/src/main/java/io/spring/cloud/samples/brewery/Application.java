package io.spring.cloud.samples.brewery;

import io.spring.cloud.samples.brewery.common.TestConfiguration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableFeignClients
@Import(TestConfiguration.class)
public class Application {

	public static void main(String[] args) {
		new SpringApplication(Application.class).run(args);
	}
}
