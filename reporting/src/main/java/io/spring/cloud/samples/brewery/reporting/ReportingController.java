package io.spring.cloud.samples.brewery.reporting;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
class ReportingController {

	private final ReportingRepository reportingRepository;

	@Autowired
	public ReportingController(ReportingRepository reportingRepository) {
		this.reportingRepository = reportingRepository;
	}

	@RequestMapping("/events/{processId}")
	public ResponseEntity<?> beerEvents(@PathVariable String processId) {
		BeerEvents beerEvents = this.reportingRepository.read(processId);
		if (beerEvents == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(beerEvents);
	}

	@RequestMapping("/events")
	public Set<Map.Entry<String, BeerEvents>> beerEvents() {
		return this.reportingRepository.read();
	}
}
