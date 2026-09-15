# Decision Log

## 1. How did you handle the concurrency race condition?

Every request locks the user's wallet row with a pessimistic write lock (`SELECT ... FOR UPDATE`) inside a single database transaction. Concurrent requests for the same wallet are forced to run one at a time, so the balance check and the deduction happen atomically and the balance can never go negative.

Idempotency uses the same lock. After acquiring it, the service checks whether the `transactionId` already exists. If it does, the request returns 409 Conflict with the original result and the balance is not touched. The `transactionId` column also has a unique constraint as a database-level safety net, and any violation is mapped to 409.

Alternatives considered:

- `synchronized` / `ReentrantLock` in Java: only works inside one JVM and breaks as soon as the service runs on more than one instance.
- Optimistic locking (`@Version`): needs retry logic, and under heavy contention on one wallet most requests fail and retry.

## 2. Where did your AI assistant give you an incorrect or sub-optimal suggestion?

The assistant generated the project and handed it over as a zip, saying it was ready to push, but it never mentioned that the project had to be imported as a Maven project in IntelliJ. When I opened the folder directly, IntelliJ treated it as a plain module and defaulted the compiler to JVM target 5. The build failed with "JDK 24 does not support the required JVM target 5", even though pom.xml clearly sets Java 17.

The fix was to right-click pom.xml, run Maven > Sync Project, and let IntelliJ pick up the Java 17 configuration from the POM. After that, all three integration tests passed.

It also could not run the test suite in its own environment, so it had never verified that the code compiled or that the concurrency tests passed. I ran the suite several times locally to confirm the results were consistent and not flaky.

Lesson: AI-generated code is a starting point, not a verified deliverable. Build setup and test runs have to be checked on your own machine.