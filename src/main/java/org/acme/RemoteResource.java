package org.acme;


import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.concurrent.CompletionStage;

import static java.time.temporal.ChronoUnit.SECONDS;

@RegisterRestClient(configKey = "remote")
@Timeout(value = 1, unit = SECONDS)
public interface RemoteResource {

    @GET
    @Path("/hello-client")
    CompletionStage<String> hello();
}
