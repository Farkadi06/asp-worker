package com.asp.worker.listener;

import com.asp.shared.dto.IngestionMetadataDto;
import com.asp.shared.dto.IngestionQueueMessage;
import com.asp.worker.client.HttpIngestionClient;
import com.asp.worker.service.IdempotencyService;
import com.asp.worker.service.S3DownloadService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.UUID;

@Component
@Slf4j
public class IngestionQueueListener {

    private final ObjectMapper objectMapper;
    private final IdempotencyService idempotencyService;
    private final HttpIngestionClient httpIngestionClient;
    private final S3DownloadService s3DownloadService;

    public IngestionQueueListener(ObjectMapper objectMapper, IdempotencyService idempotencyService, 
                                  HttpIngestionClient httpIngestionClient, S3DownloadService s3DownloadService) {
        this.objectMapper = objectMapper;
        this.idempotencyService = idempotencyService;
        this.httpIngestionClient = httpIngestionClient;
        this.s3DownloadService = s3DownloadService;
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

            // Download file from S3
            String storagePath = metadata.getStoragePath();
            if (storagePath == null || storagePath.trim().isEmpty()) {
                log.error("[WORKER] Cannot download file: storagePath is null or empty for ingestionId: {}", ingestionId);
                return;
            }

            byte[] fileBytes = s3DownloadService.downloadFile(storagePath);
            if (fileBytes == null) {
                log.error("[WORKER] Failed to download file from S3 for ingestionId: {}, storagePath: {}", ingestionId, storagePath);
                return;
            }

            Path localPdf = s3DownloadService.downloadToTemp(ingestionId, storagePath);
            if (localPdf == null) {
                log.error("[WORKER] Failed to save file to temp for ingestionId: {}, storagePath: {}", ingestionId, storagePath);
                return;
            }

            long fileSizeKB = fileBytes.length / 1024;
            log.info("[WORKER] Downloaded S3 file for ingestion {}: size={} KB, saved to {}", 
                ingestionId, fileSizeKB, localPdf);

            // Trigger processing in asp-core
            try {
                httpIngestionClient.triggerProcessing(ingestionId);
                log.info("[WORKER] Processing triggered successfully for ingestion: {}", ingestionId);
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

