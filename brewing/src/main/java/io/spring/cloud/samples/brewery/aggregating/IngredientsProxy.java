package io.spring.cloud.samples.brewery.aggregating;

import io.spring.cloud.samples.brewery.common.model.Ingredient;
import io.spring.cloud.samples.brewery.common.model.IngredientType;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(Collaborators.PROXY)
interface IngredientsProxy {

    @RequestMapping(value = "/ingredients/{ingredient}", method = RequestMethod.POST)
    Ingredient ingredients(@PathVariable("ingredient") IngredientType ingredientType,
            @RequestHeader("PROCESS-ID") String processId,
            @RequestHeader("TEST-COMMUNICATION-TYPE") String testCommunicationType);

    @RequestMapping(value = "/ingredients/api/nonExistentUrl/", method = RequestMethod.POST)
    Ingredient nonExistentIngredients(@RequestHeader("PROCESS-ID") String processId,
            @RequestHeader("TEST-COMMUNICATION-TYPE") String testCommunicationType);
}
