package io.spring.cloud.samples.brewery.ingredients;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("stubbed.ingredients")
@Data
class StubbedIngredientsProperties {

	/**
     * Set to 1000 to correlate with what comes back from config-server.
	 * The default value of threshold is 2000. The one from config-server is 1000.
	 *
	 * If value from config server is not passed then no beer will be brewed.
     */
    private Integer returnedIngredientsQuantity = 1000;
}
