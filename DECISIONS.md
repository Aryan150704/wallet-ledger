# Decision Log

## 1. How did you handle the concurrency race condition?

Every request locks the user's wallet row with a pessimistic write lock (`SELECT ... FOR UPDATE`) inside a single database transaction. Concurrent requests for the same wallet are forced to run one at a time, so the balance check and the deduction happen atomically and the balance can never go negative.

Idempotency uses the same lock. After acquiring it, the service checks whether the `transactionId` already exists. If it does, the request returns 409 Conflict with the original result and the balance is not touched. The `transactionId` column also has a unique constraint as a database-level safety net, and any violation is mapped to 409.

Alternatives considered:

- `synchronized` / `ReentrantLock` in Java: only works inside one JVM and breaks as soon as the service runs on more than one instance.
- Optimistic locking (`@Version`): needs retry logic, and under heavy contention on one wallet most requests fail and retry.

## 2. Where did your AI assistant give you an incorrect or sub-optimal suggestion?

TODO: write your own experience here before submitting.
