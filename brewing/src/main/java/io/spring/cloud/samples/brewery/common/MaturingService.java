package io.spring.cloud.samples.brewery.common;

import io.spring.cloud.samples.brewery.common.model.Ingredients;

public interface MaturingService {
    void distributeIngredients(Ingredients ingredients, String processId,
        String testCommunicationType);
}
