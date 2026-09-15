# Idempotent Wallet Transaction Processor

Spring Boot 3.5, Java 17, H2 in-memory database.

## Run the tests

Open the project in IntelliJ, right-click `TransactionProcessorIntegrationTest` and choose Run.

Or from the terminal:

```
mvn test
```

No external setup is needed.

## Endpoint

`POST /api/v1/transactions/process`

```json
{ "transactionId": "UUID", "userId": "UUID", "amount": 250.00, "type": "DEBIT" }
```

| Result | HTTP status |
|---|---|
| Processed | 200 |
| Duplicate transactionId | 409 (original result in body) |
| Insufficient funds | 422 |
| Wallet not found | 404 |
| Invalid payload | 400 |

See `DECISIONS.md` for design decisions.
