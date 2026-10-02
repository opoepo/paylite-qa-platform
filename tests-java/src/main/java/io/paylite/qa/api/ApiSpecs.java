package io.paylite.qa.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.paylite.qa.config.TestConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * Settings shared by every request to the PayLite API.
 * Attached to the specification, not to RestAssured's global state,
 * so they apply regardless of test execution order.
 */
public final class ApiSpecs {

    /** Tolerant reader: ignore fields the contract does not mention. */
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private static final RestAssuredConfig CONFIG = RestAssuredConfig.config()
            .logConfig(LogConfig.logConfig()
                    .enableLoggingOfRequestAndResponseIfValidationFails())
            .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                    .jackson2ObjectMapperFactory((type, charset) -> MAPPER));

    private ApiSpecs() {
    }

    public static RequestSpecification base() {
        return new RequestSpecBuilder()
                .setConfig(CONFIG)
                .setBaseUri(TestConfig.baseUrl())
                .setBasePath("/api/v1")
                .setContentType(ContentType.JSON)
                .build();
    }
}
