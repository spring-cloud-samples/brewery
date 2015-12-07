package io.spring.cloud.samples.brewery.presenting.feed
import groovy.transform.Canonical
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

import java.util.concurrent.atomic.AtomicInteger

@Component
class FeedRepository {

    final Set<Process> processes = Collections.synchronizedSet(new HashSet<>())

    private AtomicInteger bottles = new AtomicInteger(0)

    void addModifyProcess(String id, ProcessState newState) {
        Process p;
        if ((p = processes.find { it.id == id })) {
            p.state = newState
        } else {
            processes.add(new Process(id, newState))
        }
    }

    void setBottles(String id, Integer bottles) {
        this.bottles.set(bottles)
        addModifyProcess(id, ProcessState.DONE)
    }

    String showStatuses() {
        return "MATURING: ${countFor(ProcessState.MATURING)}\n" +
                "BOTTLING: ${countFor(ProcessState.BOTTLING)}"
    }

    Integer countFor(ProcessState state) {
        return processes.count { it.state == state }
    }

    ResponseEntity getProcessStateForId(String id) {
        Process process = processes.find { it.id == id }
        if (!process) {
            return new ResponseEntity<>("Process with id [$id] not found", HttpStatus.NOT_FOUND)
        }
        return new ResponseEntity<>(process, HttpStatus.OK)
    }

    Integer getBottles() {
        return bottles.get()
    }
}

@Canonical
class Process {
    String id
    ProcessState state
}

enum ProcessState {
    MATURING,
    BOTTLING,
    DONE
}