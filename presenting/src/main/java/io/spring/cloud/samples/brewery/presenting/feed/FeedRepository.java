package io.spring.cloud.samples.brewery.presenting.feed;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * @author Marcin Grzejszczak
 */
@Component
public class FeedRepository {
	final Set<Process> processes = Collections.synchronizedSet(new HashSet<>());

	private AtomicInteger bottles = new AtomicInteger(0);

	public void addModifyProcess(String id, ProcessState newState) {
		synchronized (processes) {
			Optional<Process> optional = processes.stream()
					.filter(process -> id.equals(process.id)).findFirst();
			if (optional.isPresent()) {
				optional.get().state = newState;
			} else {
				processes.add(new Process(id, newState));
			}
		}
	}

	public void setBottles(String id, Integer bottles) {
		this.bottles.addAndGet(bottles);
		addModifyProcess(id, ProcessState.DONE);
	}

	public String showStatuses() {
		return "MATURING: " + countFor(ProcessState.MATURING) + "\n" +
				"BOTTLING: " + countFor(ProcessState.BOTTLING);
	}

	public Long countFor(ProcessState state) {
		return processes.stream()
				.filter(process -> process.state == state)
				.count();
	}

	public ResponseEntity getProcessStateForId(String id) {
		Optional<Process> process = processes.stream()
				.filter(process1 -> id.equals(process1.id))
				.findFirst();
		if (!process.isPresent()) {
			return new ResponseEntity<>("Process with id [" + id + "] not found", HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(process.get(), HttpStatus.OK);
	}

	public Integer getBottles() {
		return bottles.get();
	}

	public Set<Process> getProcesses() {
		return this.processes;
	}
}

