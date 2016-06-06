package io.spring.cloud.samples.brewery.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanReporter;
import org.springframework.cloud.sleuth.stream.StreamSpanReporter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

import static org.springframework.cloud.sleuth.Span.hexToId;

/**
 * @author Marcin Grzejszczak
 */
@RestController
@Slf4j
public class StoringZipkinStreamSpanReporter implements SpanReporter {

	private final Multimap<Long, Span> sentSpans = Multimaps.synchronizedListMultimap(
			LinkedListMultimap.create());
	private final StreamSpanReporter delegate;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	public StoringZipkinStreamSpanReporter(StreamSpanReporter delegate) {
		this.delegate = delegate;
	}

	@RequestMapping("/spans/{traceId}")
	public ResponseEntity<Collection<Span>> spans(@PathVariable String traceId) {
		Collection<Span> spansForTrace = sentSpans.get(hexToId(traceId));
		return ResponseEntity.ok(spansForTrace);
	}

	@RequestMapping("/spans")
	public ResponseEntity<Collection<Span>> spans() {
		return ResponseEntity.ok(sentSpans.values());
	}

	@Override
	public void report(Span span) {
		sentSpans.put(span.getTraceId(), span);
		try {
			log.info("Sending span [" + objectMapper.writeValueAsString(span) + "] to Zipkin");
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		delegate.report(span);
	}

}