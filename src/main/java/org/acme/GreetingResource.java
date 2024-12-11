package org.acme;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.UUID;
import java.util.concurrent.CompletionStage;

@Path("/hello")
public class GreetingResource {

    private static final Logger logger = LoggerFactory.getLogger(GreetingResource.class);

    @Inject
    GreetingService greetingService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public CompletionStage<String> hello() {
        MDC.put("request-id", UUID.randomUUID().toString());
        logger.info("Before call in resource");
        return greetingService.hello().whenComplete((result, error) -> {
            if (error != null) {
                logger.error("Service call failed: {}", error.getMessage());
            } else {
                logger.info("Service all succeeded");
            }
        });
    }
}
