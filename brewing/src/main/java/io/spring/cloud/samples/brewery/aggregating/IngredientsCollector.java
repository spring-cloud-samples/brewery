package io.spring.cloud.samples.brewery.aggregating;

import io.micrometer.tracing.BaggageManager;
import io.spring.cloud.samples.brewery.common.TestCommunication;
import io.spring.cloud.samples.brewery.common.model.Ingredient;
import io.spring.cloud.samples.brewery.common.model.Order;
import org.slf4j.Logger;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static io.spring.cloud.samples.brewery.common.TestRequestEntityBuilder.requestEntity;
import static org.slf4j.LoggerFactory.getLogger;

class IngredientsCollector {

    private static final Logger log = getLogger(MethodHandles.lookup().lookupClass());

    private final RestTemplate restTemplate;
    private final IngredientsProxy ingredientsProxy;

    private final BaggageManager baggageManager;

    public IngredientsCollector(RestTemplate restTemplate, IngredientsProxy ingredientsProxy, BaggageManager baggageManager) {
        this.restTemplate = restTemplate;
        this.ingredientsProxy = ingredientsProxy;
        this.baggageManager = baggageManager;
    }

    List<Ingredient> collectIngredients(Order order, String processId) {
        String testCommunicationType = TestCommunication.fromBaggage(baggageManager);
        log.info("Found the following communication type [{}]", testCommunicationType);
        switch (testCommunicationType) {
        case "FEIGN":
            return callViaFeign(order, processId);
        default:
            return callViaRestTemplate(order, processId);
        }
    }

    private List<Ingredient> callViaFeign(Order order, String processId) {
        callProxyAtNonExistentUrl(() -> ingredientsProxy.nonExistentIngredients(processId, "FEIGN"));
        return order.getItems()
                .stream()
                .map(item -> ingredientsProxy.ingredients(item, processId, "FEIGN"))
                .collect(Collectors.toList());
    }

    private List<Ingredient> callViaRestTemplate(Order order, String processId) {
        callProxyAtNonExistentUrl(() -> callProxy(processId, "api/someNonExistentUrl"));
        return order.getItems()
                .stream()
                .map(item ->
                        callProxy(processId, item.name())
                )
                .collect(Collectors.toList());
    }

    private Ingredient callProxy(String processId, String name) {
        return restTemplate.exchange(requestEntity()
                .processId(processId)
                .serviceName(Collaborators.PROXY)
                .url("/ingredients/" + name)
                .httpMethod(HttpMethod.POST)
                .build(), Ingredient.class).getBody();
    }

    private Object callProxyAtNonExistentUrl(Callable<Object> runnable) {
        try {
            return runnable.call();
        }
        catch (Exception e) {
            log.error("Exception occurred while trying to call Proxy. We're doing it deliberately!", e);
            return "";
        }
    }
}
