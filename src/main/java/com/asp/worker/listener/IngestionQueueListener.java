package com.asp.worker.listener;

import com.asp.shared.dto.IngestionMetadataDto;
import com.asp.shared.dto.IngestionQueueMessage;
import com.asp.worker.client.HttpIngestionClient;
import com.asp.worker.service.IdempotencyService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class IngestionQueueListener {

    private final ObjectMapper objectMapper;
    private final IdempotencyService idempotencyService;
    private final HttpIngestionClient httpIngestionClient;

    public IngestionQueueListener(ObjectMapper objectMapper, IdempotencyService idempotencyService,
                                  HttpIngestionClient httpIngestionClient) {
        this.objectMapper = objectMapper;
        this.idempotencyService = idempotencyService;
        this.httpIngestionClient = httpIngestionClient;
    }

    public void handleMessage(String messageId, String body) {
        log.info("[WORKER] Received SQS message: messageId={}, body={}", messageId, body);

        try {
            if (idempotencyService.hasProcessed(messageId)) {
                log.info("[WORKER] Duplicate message ignored: {}", messageId);
                return;
            }

            IngestionQueueMessage message = parseMessage(messageId, body);
            if (message == null) {
                return;
            }

            if (!hasRequiredFields(message)) {
                log.error("[WORKER] Missing required fields for messageId={}", messageId);
                return;
            }

            log.info(
                "[WORKER] Parsed message: ingestionId={}, tenantId={}, attempt={}",
                message.getIngestionId(),
                message.getTenantId(),
                message.getAttempt()
            );
            log.info("[WORKER] Message context: messageId={}, traceId={}", messageId, message.getTraceId());

            // Fetch ingestion metadata from asp-core
            UUID ingestionId = message.getIngestionId();
            IngestionMetadataDto metadata = httpIngestionClient.fetchMetadata(ingestionId);

            log.info("[WORKER] Loaded ingestion metadata: tenantId={}, file={}, type={}, source={}, status={}, fileSize={}",
                metadata.getTenantId(),
                metadata.getOriginalFileName(),
                metadata.getIngestionType(),
                metadata.getSource(),
                metadata.getStatus(),
                metadata.getFileSize()
            );

            // Trigger processing in asp-core
            try {
                log.info("[WORKER] Triggering processing for ingestion {}", ingestionId);
                httpIngestionClient.triggerProcessing(ingestionId);
                log.info("[WORKER] Processing triggered successfully for ingestion {}", ingestionId);
            } catch (Exception e) {
                log.error("[WORKER] Failed to trigger processing for ingestionId: {}", ingestionId, e);
                return;
            }

            idempotencyService.markProcessed(messageId);
            log.info("[WORKER] Marked message as processed: {}", messageId);
        } catch (Exception e) {
            log.error("[WORKER] Unexpected error processing messageId={}", messageId, e);
        }
    }

    private IngestionQueueMessage parseMessage(String messageId, String body) {
        try {
            return objectMapper.readValue(body, IngestionQueueMessage.class);
        } catch (JsonProcessingException e) {
            log.error("[WORKER] Failed to parse JSON for messageId={}", messageId, e);
            return null;
        }
    }

    private boolean hasRequiredFields(IngestionQueueMessage message) {
        return message.getIngestionId() != null
            && message.getTenantId() != null
            && message.getFilePath() != null;
    }
}

