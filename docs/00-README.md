# asp-worker

`asp-worker` is the asynchronous ingestion worker for the ASP platform.  
It is responsible for consuming ingestion messages from AWS SQS and will, in later steps, orchestrate the parsing/enrichment workflow outside of the API runtime.

## Current scope

- Spring Boot 3.3 / Java 17 application
- AWS SDK v2 SQS listener skeleton
- DTO definition that matches the message emitted by `asp-core`
- Logging-only handling (no persistence, parsing, or enrichment logic yet)

## Future roadmap

1. Download source documents from storage
2. Invoke parser + enrichment pipelines
3. Persist results and publish completion events
4. Add observability, retries, and dead-letter handling

