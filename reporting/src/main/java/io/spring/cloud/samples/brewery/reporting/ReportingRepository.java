package io.spring.cloud.samples.brewery.reporting;

import io.spring.cloud.samples.brewery.common.events.Event;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
class ReportingRepository {

	private final Map<String, BeerEvents> eventsDatabase = new ConcurrentHashMap<>();

	public void createOrUpdate(Event event) {
		BeerEvents beerEvents = eventsDatabase.getOrDefault(event.getProcessId(), new BeerEvents());
		LocalDateTime time = event.getEventTime();
		String convertedTime = DateTimeFormatter.ISO_DATE_TIME.format(time);
		switch(event.getEventType()) {
			case INGREDIENTS_ORDERED:
				beerEvents.setIngredientsOrderedTime(convertedTime);
				break;
			case BREWING_STARTED:
				beerEvents.setBrewingStartedTime(convertedTime);
				break;
			case BEER_MATURED:
				beerEvents.setBeerMaturedTime(convertedTime);
				break;
			case BEER_BOTTLED:
				beerEvents.setBeerBottledTime(convertedTime);
				break;
		}
		eventsDatabase.put(event.getProcessId(), beerEvents);
	}

	public BeerEvents read(String processId) {
		return eventsDatabase.get(processId);
	}


	public Set<Map.Entry<String, BeerEvents>> read() {
		return eventsDatabase.entrySet();
	}
}
