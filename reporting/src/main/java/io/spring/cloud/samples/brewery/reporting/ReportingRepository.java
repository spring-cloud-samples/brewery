package io.spring.cloud.samples.brewery.reporting;

import io.spring.cloud.samples.brewery.common.events.Event;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class ReportingRepository {

	private final Map<String, BeerEvents> eventsDatabase = new ConcurrentHashMap<>();

	public void createOrUpdate(Event event) {
		BeerEvents beerEvents = eventsDatabase.getOrDefault(event.getProcessId(), new BeerEvents());
		switch(event.getEventType()) {
			case INGREDIENTS_ORDERED:
				beerEvents.setIngredientsOrderedTime(event.getEventTime());
				break;
			case BREWING_STARTED:
				beerEvents.setBrewingStartedTime(event.getEventTime());
				break;
			case BEER_MATURED:
				beerEvents.setBeerMaturedTime(event.getEventTime());
				break;
			case BEER_BOTTLED:
				beerEvents.setBeerBottledTime(event.getEventTime());
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
