package io.spring.cloud.samples.brewery.presenting.config;

public final class Versions {
    private Versions() {
        throw new UnsupportedOperationException("Can't instantiate a utility class");
    }

    public static final String PRESENTING_JSON_VERSION_1 = "application/vnd.io.spring.cloud.presenting.v1+json";

    public static final String BREWING_CONTENT_TYPE_V1 = "application/vnd.io.spring.cloud.brewing.v1+json";
}
