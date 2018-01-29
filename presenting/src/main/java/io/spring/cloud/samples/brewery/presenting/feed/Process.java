package io.spring.cloud.samples.brewery.presenting.feed;

public class Process {
	public String id;
	public ProcessState state;

	public Process(String id, ProcessState state) {
		this.id = id;
		this.state = state;
	}

	public Process() {
	}
}