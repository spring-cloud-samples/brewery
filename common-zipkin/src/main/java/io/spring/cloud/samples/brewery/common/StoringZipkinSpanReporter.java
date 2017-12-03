package io.spring.cloud.samples.brewery.common;

import java.util.Collection;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanReporter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Marcin Grzejszczak
 */
@Slf4j
public class StoringZipkinSpanReporter implements SpanReporter {

	final Multimap<Long, Span> sentSpans = Multimaps.synchronizedListMultimap(
			LinkedListMultimap.create());
	private final SpanReporter delegate;

	public StoringZipkinSpanReporter(SpanReporter spanReporter) {
		delegate = spanReporter;
	}



	@Override
	public void report(Span span) {
		sentSpans.put(span.getTraceId(), span);
		log.info("Sending span [" + span.toString() + "] to Zipkin");
		delegate.report(span);
	}
}

@RestController
class SpansController {

	private final StoringZipkinSpanReporter reporter;

	SpansController(StoringZipkinSpanReporter storingZipkinSpanReporter) {
		this.reporter = storingZipkinSpanReporter;
	}

	@RequestMapping("/spans/{traceId}")
	public ResponseEntity<Collection<Span>> spans(@PathVariable String traceId) {
		Collection<Span> spansForTrace = reporter.sentSpans
				.get(Span.hexToId(traceId));
		return ResponseEntity.ok(spansForTrace);
	}

	@RequestMapping("/spans")
	public ResponseEntity<Collection<Span>> spans() {
		Collection<Span> spansForTrace = reporter.sentSpans.values();
		return ResponseEntity.ok(spansForTrace);
	}
}