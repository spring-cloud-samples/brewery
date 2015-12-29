package io.spring.cloud.samples.brewery.reporting;

import lombok.Data;

@Data
class BeerEvents {
	private String ingredientsOrderedTime;
	private String brewingStartedTime;
	private String beerMaturedTime;
	private String beerBottledTime;
}
