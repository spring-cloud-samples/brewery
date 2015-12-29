package io.spring.cloud.samples.brewery.reporting;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.spring.cloud.samples.brewery.common.events.EventSink;

@SpringBootApplication
@Configuration
@EnableDiscoveryClient
@RestController
@EnableBinding(EventSink.class)
public class Application {

    @Autowired ReportingRepository reportingRepository;

    public static void main(String[] args) {
        new SpringApplication(Application.class).run(args);
    }

    @RequestMapping("/events/{processId}")
    public ResponseEntity beerEvents(@PathVariable String processId) {
        BeerEvents beerEvents = reportingRepository.read(processId);
        if (beerEvents == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(beerEvents);
    }

    @RequestMapping("/events")
    public Set<Map.Entry<String, BeerEvents>> beerEvents() {
        return reportingRepository.read();
    }
}
