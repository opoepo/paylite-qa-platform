package io.paylite.qa.api;

import io.paylite.qa.config.TestConfig;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * Settings shared by every request to the PayLite API.
 */
public final class ApiSpecs {

    static {
        // Full request and response are printed only when a check fails:
        // silent when green, everything needed to debug when red.
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    private ApiSpecs() {
    }

    public static RequestSpecification base() {
        return new RequestSpecBuilder()
                .setBaseUri(TestConfig.baseUrl())
                .setBasePath("/api/v1")
                .setContentType(ContentType.JSON)
                .build();
    }
}
