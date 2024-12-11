package org.acme;

import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.eclipse.microprofile.faulttolerance.Timeout;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import static java.util.concurrent.CompletableFuture.failedStage;

@Timeout
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class RunOnContextTimeoutInterceptor {

    public static final String TIMEOUT_INTERCEPTOR_ENABLED = "timeout.interceptor.enabled";

    public boolean enabled() {
        return Boolean.getBoolean(TIMEOUT_INTERCEPTOR_ENABLED);
    }

    @AroundInvoke
    public Object runOnVertxContextAfterTimeout(InvocationContext invocationContext) throws Throwable {
        if (enabled() && returnsCompletionStage(invocationContext) && VertxContext.isOnDuplicatedContext()) {
            var context = VertxContext.getOrCreateDuplicatedContext();
            return proceedWithStage(invocationContext)
                    .exceptionallyComposeAsync(CompletableFuture::failedStage, runOnContext(context));
        }
        return invocationContext.proceed();
    }

    private boolean returnsCompletionStage(InvocationContext invocationContext) {
        return invocationContext.getMethod().getReturnType().isAssignableFrom(CompletionStage.class);
    }

    private Executor runOnContext(Context context) {
        return task -> context.runOnContext(ignored -> task.run());
    }

    private CompletionStage<?> proceedWithStage(InvocationContext invocationContext) {
        try {
            return (CompletionStage<?>) invocationContext.proceed();
        } catch (Exception e) {
            return failedStage(e);
        }
    }
}
