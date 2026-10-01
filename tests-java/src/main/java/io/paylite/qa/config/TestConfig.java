package io.paylite.qa.config;

/**
 * Where the tests look for the service.
 * Priority: -Dpaylite.baseUrl > PAYLITE_BASE_URL env var > local default.
 */
public final class TestConfig {

    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    private TestConfig() {
    }

    public static String baseUrl() {
        String fromProperty = System.getProperty("paylite.baseUrl");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty;
        }
        String fromEnv = System.getenv("PAYLITE_BASE_URL");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return DEFAULT_BASE_URL;
    }
}
