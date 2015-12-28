package io.spring.cloud.samples.brewery.common.events;

import lombok.Data;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;

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

		public Message<Event> build() {
			return MessageBuilder.withPayload(new Event(processId, eventType)).build();
		}

	}
}
