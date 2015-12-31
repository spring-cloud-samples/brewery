package io.spring.cloud.samples.brewery.aggregating;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@ConfigurationProperties("ingredients")
@Data
class IngredientsProperties {

    private Integer threshold = 2000;

}
