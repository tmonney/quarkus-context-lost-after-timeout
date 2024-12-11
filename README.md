# context-lost-after-timeout

This is a reproducer for an issue (https://github.com/quarkusio/quarkus/issues/45061) where the Vert.x context is somehow lost after a rest client call returning a 
`CompletionStage` and configured with Smallrye Fault Tolerance times out.
This causes e.g. logs to lack previously set MDC fields.

As a workaround, we wrote an interceptor for `@Timeout`-annotated methods that "puts back" the failed completion stage 
on the original Vert.x duplicated context.

The `GreetingResourceTest` shows what's happening. A `request-id` MDC field is used and added to the console log pattern.   

## Timeout example:
```
2024-12-11 10:54:45,479 INFO  [35691c12-63ab-4ed7-b77d-ac156b4b2f5e] [org.acm.GreetingResource] (vert.x-eventloop-thread-0) Before call in resource
2024-12-11 10:54:45,516 INFO  [35691c12-63ab-4ed7-b77d-ac156b4b2f5e] [org.acm.GreetingService] (vert.x-eventloop-thread-0) Before call in service
2024-12-11 10:54:46,545 ERROR [                                    ] [org.acm.GreetingService] (executor-thread-1) Remote call failed: org.acme.RemoteResource$$CDIWrapper#hello timed out
2024-12-11 10:54:46,545 ERROR [                                    ] [org.acm.GreetingResource] (executor-thread-1) Service call failed: org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException: org.acme.RemoteResource$$CDIWrapper#hello timed out
2024-12-11 10:54:46,550 ERROR [35691c12-63ab-4ed7-b77d-ac156b4b2f5e] [io.qua.ver.htt.run.QuarkusErrorHandler] (vert.x-eventloop-thread-0) HTTP Request to /hello failed, error id: 82bc6c66-7c56-468b-8970-ec2d0370a531-1: org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException: org.acme.RemoteResource$$CDIWrapper#hello timed out
	at io.smallrye.faulttolerance.core.timeout.Timeout.timeoutException(Timeout.java:93)
	at io.smallrye.faulttolerance.core.timeout.CompletionStageTimeout.lambda$doApply$0(CompletionStageTimeout.java:41)
	at io.smallrye.faulttolerance.core.timeout.TimeoutExecution.timeoutAndInterrupt(TimeoutExecution.java:68)
	at io.smallrye.context.impl.wrappers.SlowContextualRunnable.run(SlowContextualRunnable.java:19)
	at io.smallrye.faulttolerance.core.timer.ThreadTimer$Task.run(ThreadTimer.java:194)
	at io.smallrye.context.impl.wrappers.SlowContextualRunnable.run(SlowContextualRunnable.java:19)
	at io.quarkus.vertx.core.runtime.VertxCoreRecorder$15.runWith(VertxCoreRecorder.java:642)
	at org.jboss.threads.EnhancedQueueExecutor$Task.doRunWith(EnhancedQueueExecutor.java:2675)
	at org.jboss.threads.EnhancedQueueExecutor$Task.run(EnhancedQueueExecutor.java:2654)
	at org.jboss.threads.EnhancedQueueExecutor.runThreadBody(EnhancedQueueExecutor.java:1627)
	at org.jboss.threads.EnhancedQueueExecutor$ThreadBody.run(EnhancedQueueExecutor.java:1594)
	at org.jboss.threads.DelegatingRunnable.run(DelegatingRunnable.java:11)
	at org.jboss.threads.ThreadLocalResettingRunnable.run(ThreadLocalResettingRunnable.java:11)
	at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
	at java.base/java.lang.Thread.run(Thread.java:1575)

```
The error logs are lacking the MDC field and the thread used is not a Quarkus event loop or worker thread.

# Timeout with the interceptor
```
2024-12-11 10:54:46,609 INFO  [4d97a6c5-8764-44af-acbd-5a3eefc5c309] [org.acm.GreetingResource] (vert.x-eventloop-thread-1) Before call in resource
2024-12-11 10:54:46,609 INFO  [4d97a6c5-8764-44af-acbd-5a3eefc5c309] [org.acm.GreetingService] (vert.x-eventloop-thread-1) Before call in service
2024-12-11 10:54:47,611 ERROR [4d97a6c5-8764-44af-acbd-5a3eefc5c309] [org.acm.GreetingService] (vert.x-eventloop-thread-1) Remote call failed: org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException: org.acme.RemoteResource$$CDIWrapper#hello timed out
2024-12-11 10:54:47,611 ERROR [4d97a6c5-8764-44af-acbd-5a3eefc5c309] [org.acm.GreetingResource] (vert.x-eventloop-thread-1) Service call failed: org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException: org.acme.RemoteResource$$CDIWrapper#hello timed out
2024-12-11 10:54:47,612 ERROR [4d97a6c5-8764-44af-acbd-5a3eefc5c309] [io.qua.ver.htt.run.QuarkusErrorHandler] (vert.x-eventloop-thread-1) HTTP Request to /hello failed, error id: 82bc6c66-7c56-468b-8970-ec2d0370a531-2: org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException: org.acme.RemoteResource$$CDIWrapper#hello timed out
	at io.smallrye.faulttolerance.core.timeout.Timeout.timeoutException(Timeout.java:93)
	at io.smallrye.faulttolerance.core.timeout.CompletionStageTimeout.lambda$doApply$0(CompletionStageTimeout.java:41)
	at io.smallrye.faulttolerance.core.timeout.TimeoutExecution.timeoutAndInterrupt(TimeoutExecution.java:68)
	at io.smallrye.context.impl.wrappers.SlowContextualRunnable.run(SlowContextualRunnable.java:19)
	at io.smallrye.faulttolerance.core.timer.ThreadTimer$Task.run(ThreadTimer.java:194)
	at io.smallrye.context.impl.wrappers.SlowContextualRunnable.run(SlowContextualRunnable.java:19)
	at io.quarkus.vertx.core.runtime.VertxCoreRecorder$15.runWith(VertxCoreRecorder.java:642)
	at org.jboss.threads.EnhancedQueueExecutor$Task.doRunWith(EnhancedQueueExecutor.java:2675)
	at org.jboss.threads.EnhancedQueueExecutor$Task.run(EnhancedQueueExecutor.java:2654)
	at org.jboss.threads.EnhancedQueueExecutor.runThreadBody(EnhancedQueueExecutor.java:1627)
	at org.jboss.threads.EnhancedQueueExecutor$ThreadBody.run(EnhancedQueueExecutor.java:1594)
	at org.jboss.threads.DelegatingRunnable.run(DelegatingRunnable.java:11)
	at org.jboss.threads.ThreadLocalResettingRunnable.run(ThreadLocalResettingRunnable.java:11)
	at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
	at java.base/java.lang.Thread.run(Thread.java:1575)
```
All messages are logged with their MDC field set and the thread used is the event loop thread.

# No timeout
For completeness, an example with no timeout.
```
2024-12-11 10:54:47,633 INFO  [6eeb88d6-1e8f-486b-86eb-add44f8f7aba] [org.acm.GreetingResource] (vert.x-eventloop-thread-0) Before call in resource
2024-12-11 10:54:47,633 INFO  [6eeb88d6-1e8f-486b-86eb-add44f8f7aba] [org.acm.GreetingService] (vert.x-eventloop-thread-0) Before call in service
2024-12-11 10:54:47,652 INFO  [6eeb88d6-1e8f-486b-86eb-add44f8f7aba] [org.acm.GreetingService] (vert.x-eventloop-thread-0) Remote call succeeded
2024-12-11 10:54:47,652 INFO  [6eeb88d6-1e8f-486b-86eb-add44f8f7aba] [org.acm.GreetingResource] (vert.x-eventloop-thread-0) Service all succeeded
```
