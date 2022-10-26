package io.spring.cloud.samples.brewery.maturing;

import io.spring.cloud.samples.brewery.common.MaturingService;
import io.spring.cloud.samples.brewery.common.model.Ingredients;
import org.slf4j.Logger;

import org.springframework.stereotype.Service;

@Service
class Maturer implements MaturingService {

	private static final Logger log = org.slf4j.LoggerFactory.getLogger(Maturer.class);
	private final BottlingServiceUpdater bottlingServiceUpdater;

	Maturer(BottlingServiceUpdater bottlingServiceUpdater) {
		this.bottlingServiceUpdater = bottlingServiceUpdater;
	}

	@Override
	public void distributeIngredients(Ingredients ingredients, String processId, String testCommunicationType) {
		log.info("I'm in the maturing service. Will distribute ingredients");
		bottlingServiceUpdater.updateBottlingServiceAboutBrewedBeer(ingredients, processId);
	}
}
