package io.spring.cloud.samples.brewery.common.events;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Event implements Serializable {
	private String processId;
	private EventType eventType;
	private LocalDateTime eventTime = LocalDateTime.now();

	public Event(String processId, EventType eventType) {
		this.processId = processId;
		this.eventType = eventType;
	}

	public Event() {}

	public static EventBuilder builder() {
		return new EventBuilder();
	}

	public static class EventBuilder {

		private String processId;
		private EventType eventType;

		public EventBuilder eventType(EventType eventType) {
			this.eventType = eventType;
			return this;
		}

		public EventBuilder processId(String processId) {
			this.processId = processId;
			return this;
		}

		public Event build() {
			return new Event(this.processId, this.eventType);
		}

	}
}
