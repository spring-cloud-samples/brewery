package io.spring.cloud.samples.brewery.zookeeper;

import static com.netflix.hystrix.HystrixCommand.Setter.withGroupKey;
import static com.netflix.hystrix.HystrixCommandGroupKey.Factory.asKey;

import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import io.spring.cloud.samples.brewery.zookeeper.model.Ingredients;
import org.springframework.cloud.sleuth.TraceManager;
import org.springframework.cloud.sleuth.instrument.hystrix.TraceCommand;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.HystrixCommandKey;
import lombok.extern.slf4j.Slf4j;
import io.spring.cloud.samples.brewery.zookeeper.model.Ingredient;
import io.spring.cloud.samples.brewery.zookeeper.model.Order;

@Slf4j
class IngredientsAggregator {

    private final IngredientsProperties ingredientsProperties;
    private final MaturingServiceUpdater maturingUpdater;
    private final IngredientWarehouse ingredientWarehouse;
    private final AsyncRestTemplate asyncRestTemplate;
    private final TraceManager traceManager;

    IngredientsAggregator(IngredientsProperties ingredientsProperties,
                          IngredientWarehouse ingredientWarehouse,
                          TraceManager traceManager, AsyncRestTemplate asyncRestTemplate,
                          MaturingServiceClient maturingServiceClient, RestTemplate restTemplate) {
        this.ingredientWarehouse = ingredientWarehouse;
        this.asyncRestTemplate = asyncRestTemplate;
        this.traceManager = traceManager;
        this.maturingUpdater = new MaturingServiceUpdater(ingredientsProperties,
                ingredientWarehouse, maturingServiceClient, restTemplate);
        this.ingredientsProperties = ingredientsProperties;
    }

    // TODO: Consider simplifying the case by removing the DB (always matches threshold)
    Ingredients fetchIngredients(Order order, String processId) {
        List<ListenableFuture<ResponseEntity<Ingredient>>> futures = ingredientsProperties
                .getListOfServiceNames(order)
                .stream()
                .map(this::harvest)
                .collect(Collectors.toList());
        List<Ingredient> allIngredients = futures.stream()
                .map(this::getUnchecked)
                .map(HttpEntity::getBody)
                .collect(Collectors.toList());
        allIngredients.stream()
                .filter(ingredient -> ingredient != null)
                .forEach(ingredientWarehouse::addIngredient);
        Ingredients ingredients = ingredientWarehouse.getCurrentState();
        return maturingUpdater.updateIfLimitReached(ingredients, processId);
    }

    private <T> T getUnchecked(Future<T> future) {
        try {
            return future.get();
        } catch (Exception e) {
            log.error("Exception occurred while trying to get the future", e);
        }
        return null;
    }

    ListenableFuture<ResponseEntity<Ingredient>> harvest(String service) {
        TraceCommand<ListenableFuture<ResponseEntity<Ingredient>>> traceCommand = new TraceCommand<ListenableFuture<ResponseEntity<Ingredient>>>(traceManager,
                withGroupKey(asKey(service)).andCommandKey(HystrixCommandKey.Factory.asKey(service + "_command"))) {
            @Override
            public ListenableFuture<ResponseEntity<Ingredient>> doRun() throws Exception {
                return asyncRestTemplate.getForEntity(ingredientsProperties.getRootUrl() + "/" + service,
                        Ingredient.class);
            }

            @Override
            protected ListenableFuture<ResponseEntity<Ingredient>> getFallback() {
                log.error("Can't connect to {}", service);
                return null;
            }
        };
        try {
            return traceCommand.doRun();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
