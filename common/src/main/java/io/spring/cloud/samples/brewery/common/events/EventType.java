package io.spring.cloud.samples.brewery.common.events;

import java.io.Serializable;

public enum EventType {
	INGREDIENTS_ORDERED,
	BREWING_STARTED,
	BEER_MATURED,
	BEER_BOTTLED
}
