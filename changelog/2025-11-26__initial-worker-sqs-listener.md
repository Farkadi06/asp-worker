## 2025-11-26 — Initial Worker SQS Listener

- Scaffolded standalone Spring Boot 3.3 project for `asp-worker`.
- Added AWS SDK v2 SQS client configuration with environment-based queue URL.
- Implemented logging-only listener and DTO that mirrors `asp-core` ingestion messages.
- Added docs describing setup, message format, and listener lifecycle.

