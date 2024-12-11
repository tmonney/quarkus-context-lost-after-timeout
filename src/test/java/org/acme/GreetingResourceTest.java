package org.acme;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.quarkiverse.wiremock.devservice.ConnectWireMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.acme.RunOnContextTimeoutInterceptor.TIMEOUT_INTERCEPTOR_ENABLED;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
@ConnectWireMock
class GreetingResourceTest {

    WireMock wireMock;

    @Test
    void testSuccess() {
        wireMock.register(get(urlPathEqualTo("/hello-client"))
                .willReturn(ok("Hello World")));

        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("Hello World"));
    }

    @Test
    void testTimeout_interceptorEnabled() {
        System.setProperty(TIMEOUT_INTERCEPTOR_ENABLED, "true");

        wireMock.register(get(urlPathEqualTo("/hello-client"))
                .willReturn(ok("Hello World")
                        .withFixedDelay(2000))); // will cause a timeout

        given()
                .when().get("/hello")
                .then()
                .statusCode(500);
    }

    @Test
    void testTimeout_interceptorDisabled() {
        System.setProperty(TIMEOUT_INTERCEPTOR_ENABLED, "false");

        wireMock.register(get(urlPathEqualTo("/hello-client"))
                .willReturn(ok("Hello World")
                        .withFixedDelay(2000))); // will cause a timeout

        given()
                .when().get("/hello")
                .then()
                .statusCode(500);
    }
}