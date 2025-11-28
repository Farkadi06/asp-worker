# Worker Project Setup

This document explains how to bootstrap and run the `asp-worker` service locally.

## Prerequisites

- JDK 17
- AWS CLI (optional, for sending test messages)
- Access to an AWS SQS queue (or a mocked endpoint) whose URL will be provided via the `ASP_SQS_INGESTION_QUEUE_URL` environment variable.
- `asp-core` service running (for metadata fetch)

## Environment Variables

Required:
- `ASP_SQS_INGESTION_QUEUE_URL` - SQS queue URL for ingestion messages
- `ASP_INTERNAL_ACCESS_TOKEN` - Token for internal API authentication (must match `ASP_INTERNAL_ACCESS_TOKEN` in asp-core)
- `ASP_S3_BUCKET_NAME` - S3 bucket name for ingestion files (e.g., `asp-prod-ingestions`)
- `AWS_REGION` - AWS region for S3 (e.g., `eu-west-1`)

Optional:
- `ASP_CORE_BASE_URL` - Base URL for asp-core API (default: `http://localhost:8080`)

Note: AWS credentials are loaded from the default credential provider chain (environment variables, IAM roles, etc.)

## Running Locally

```bash
cd asp-worker
set ASP_SQS_INGESTION_QUEUE_URL=https://sqs.us-east-1.amazonaws.com/123456789012/asp-ingestion-queue
set ASP_CORE_BASE_URL=http://localhost:8080
set ASP_INTERNAL_ACCESS_TOKEN=your_internal_token_here
set ASP_S3_BUCKET_NAME=asp-prod-ingestions
set AWS_REGION=eu-west-1
./gradlew bootRun
```

Expected logs:

```
[WORKER] Listening on SQS queue: https://sqs...
```

## Sending a test message

```bash
aws sqs send-message \
  --queue-url $ASP_SQS_INGESTION_QUEUE_URL \
  --message-body '{"version":"1","ingestionId":"9e9058e1-9ae9-4713-9531-78b74d798913","tenantId":"550e8400-e29b-41d4-a716-446655440000","filePath":"tenants/.../original.pdf","createdAt":"2025-11-25T08:00:00Z","traceId":"trace-123","attempt":1}'
```

Worker output:

```
[WORKER] Received message: {"version":"1", ...}
```

