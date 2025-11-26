# SQS Listener Overview

## Message contract

| Field        | Type    | Description                                   |
|--------------|---------|-----------------------------------------------|
| `version`    | String  | Message schema version                        |
| `ingestionId`| UUID    | Ingestion identifier                          |
| `tenantId`   | UUID    | Tenant identifier                             |
| `filePath`   | String  | Path to the original uploaded document        |
| `createdAt`  | Instant | Timestamp when the message was queued         |
| `traceId`    | String  | Trace identifier for cross-service debugging  |
| `attempt`    | Number  | Delivery attempt counter                      |

The message format mirrors the payload emitted by `asp-core`’s `SqsIngestionPublisher`.

## Listener lifecycle

1. `SqsListenerConfig` instantiates `SqsClient` using AWS SDK v2.
2. The ingestion queue URL is loaded from the `ASP_SQS_INGESTION_QUEUE_URL` environment variable.
3. `IngestionQueueListener.handleMessage(String body)` receives the raw JSON body and logs it.
4. Future iterations will deserialize into `IngestionQueueMessage` and trigger processing steps.

## Failure handling (future work)

- Automatic retries on transient SQS failures.
- Dead-letter queue routing for poison messages.
- Metrics and tracing hooks to surface throughput and error rates.

