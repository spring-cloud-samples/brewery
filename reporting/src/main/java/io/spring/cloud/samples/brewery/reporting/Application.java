package io.spring.cloud.samples.brewery.reporting;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@SpringBootApplication
@Configuration
@EnableDiscoveryClient
@IntegrationComponentScan
@RestController
public class Application {

    @Autowired ReportingRepository reportingRepository;

    public static void main(String[] args) {
        new SpringApplication(Application.class).run(args);
    }

    @RequestMapping("/events/{processId}")
    ResponseEntity beerEvents(@PathVariable String processId) {
        BeerEvents beerEvents = reportingRepository.read(processId);
        if (beerEvents == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(beerEvents);
    }

    @RequestMapping("/events")
    Set<Map.Entry<String, BeerEvents>> beerEvents() {
        return reportingRepository.read();
    }
}
