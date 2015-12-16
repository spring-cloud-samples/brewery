package io.spring.cloud.samples.brewery.eureka;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

import org.apache.commons.io.IOUtils;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

class ExternalServicesStub implements Closeable {

    private final WireMockServer wireMockServer;

    ExternalServicesStub(IngredientsProperties ingredientsProperties) throws IOException {
        URI uri = URI.create(ingredientsProperties.getRootUrl());
        this.wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig()
                .port(uri.getPort()));
        wireMockServer.addStubMapping(StubMapping.buildFrom(
                IOUtils.toString(ExternalServicesStub.class.getResourceAsStream("/mappings/hop.json"))));
        wireMockServer.addStubMapping(StubMapping.buildFrom(
                IOUtils.toString(ExternalServicesStub.class.getResourceAsStream("/mappings/yeast.json"))));
        wireMockServer.addStubMapping(StubMapping.buildFrom(
                IOUtils.toString(ExternalServicesStub.class.getResourceAsStream("/mappings/malt.json"))));
        wireMockServer.addStubMapping(StubMapping.buildFrom(
                IOUtils.toString(ExternalServicesStub.class.getResourceAsStream("/mappings/water.json"))));
    }

    void start() {
        wireMockServer.start();
    }

    @Override
    public void close() throws IOException {
        wireMockServer.stop();
    }
}
