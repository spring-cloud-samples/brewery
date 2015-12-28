package io.spring.cloud.samples.brewery.reporting;

import lombok.Data;

import java.time.LocalDateTime;

@Data
class BeerEvents {
	private LocalDateTime ingredientsOrderedTime;
	private LocalDateTime brewingStartedTime;
	private LocalDateTime beerMaturedTime;
	private LocalDateTime beerBottledTime;
}
