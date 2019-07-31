package io.spring.cloud.samples.brewery.common.events;

import java.io.Serializable;
import java.time.LocalDateTime;

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

	public String getProcessId() {
		return this.processId;
	}

	public EventType getEventType() {
		return this.eventType;
	}

	public LocalDateTime getEventTime() {
		return this.eventTime;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}

	public void setEventType(EventType eventType) {
		this.eventType = eventType;
	}

	public void setEventTime(LocalDateTime eventTime) {
		this.eventTime = eventTime;
	}

	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof Event)) return false;
		final Event other = (Event) o;
		if (!other.canEqual((Object) this)) return false;
		final Object this$processId = this.getProcessId();
		final Object other$processId = other.getProcessId();
		if (this$processId == null ? other$processId != null : !this$processId.equals(other$processId)) return false;
		final Object this$eventType = this.getEventType();
		final Object other$eventType = other.getEventType();
		if (this$eventType == null ? other$eventType != null : !this$eventType.equals(other$eventType)) return false;
		final Object this$eventTime = this.getEventTime();
		final Object other$eventTime = other.getEventTime();
		if (this$eventTime == null ? other$eventTime != null : !this$eventTime.equals(other$eventTime)) return false;
		return true;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof Event;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $processId = this.getProcessId();
		result = result * PRIME + ($processId == null ? 43 : $processId.hashCode());
		final Object $eventType = this.getEventType();
		result = result * PRIME + ($eventType == null ? 43 : $eventType.hashCode());
		final Object $eventTime = this.getEventTime();
		result = result * PRIME + ($eventTime == null ? 43 : $eventTime.hashCode());
		return result;
	}

	public String toString() {
		return "Event(processId=" + this.getProcessId() + ", eventType=" + this.getEventType() + ", eventTime=" + this
				.getEventTime() + ")";
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
