package io.spring.cloud.samples.brewery.aggregating;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.spring.cloud.samples.brewery.aggregating.model.IngredientType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import com.google.common.collect.ImmutableMap;
import lombok.Data;
import io.spring.cloud.samples.brewery.aggregating.model.Order;

@ConfigurationProperties("ingredients")
@Data
public class IngredientsProperties {

    private Map<IngredientType, String> serviceNames = ImmutableMap.<IngredientType, String>builder()
            .put(IngredientType.WATER, IngredientType.WATER.name().toLowerCase())
            .put(IngredientType.MALT, IngredientType.MALT.name().toLowerCase())
            .put(IngredientType.HOP, IngredientType.HOP.name().toLowerCase())
            .put(IngredientType.YEAST, IngredientType.YEAST.name().toLowerCase())
            .build();
    private Integer threshold = 2000;

	/**
     * Set to 1000 to correlate with what comes back from config-server
     */
    private Integer returnedIngredientsQuantity = 1000;

    public List<String> getListOfServiceNames(Order order) {
        return serviceNames.entrySet()
                .stream()
                .filter((entry -> order.getItems().contains(entry.getKey())))
                .map((Map.Entry::getValue))
                .collect(Collectors.toList());
    }
}
