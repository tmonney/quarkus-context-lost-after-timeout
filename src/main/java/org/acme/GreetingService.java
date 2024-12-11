package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class GreetingService {

    private static final Logger logger = LoggerFactory.getLogger(GreetingService.class);

    @Inject
    @RestClient
    RemoteResource remoteResource;

    CompletionStage<String> hello() {
        logger.info("Before call in service");
        return remoteResource.hello().whenComplete((result, error) -> {
            if (error != null) {
                logger.error("Remote call failed: {}", error.getMessage());
            } else {
                logger.info("Remote call succeeded");
            }
        });
    }
}
