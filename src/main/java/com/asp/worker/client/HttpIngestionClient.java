package com.asp.worker.client;

import com.asp.shared.dto.IngestionMetadataDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class HttpIngestionClient {

    private final WebClient webClient;

    @Value("${ASP_INTERNAL_ACCESS_TOKEN:}")
    private String internalToken;

    public IngestionMetadataDto fetchMetadata(UUID ingestionId) {
        log.debug("[WORKER] Fetching ingestion metadata for ingestionId: {}", ingestionId);

        try {
            IngestionMetadataDto metadata = webClient.get()
                .uri("/internal/ingestions/{ingestionId}", ingestionId)
                .header("X-Internal-Token", internalToken)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), response -> {
                    if (response.statusCode() == HttpStatus.NOT_FOUND) {
                        log.warn("[WORKER] Ingestion not found: {}", ingestionId);
                        return Mono.error(new RuntimeException("Ingestion not found: " + ingestionId));
                    }
                    log.error("[WORKER] Client error fetching metadata for ingestionId: {}, status: {}", 
                        ingestionId, response.statusCode());
                    return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
                })
                .onStatus(status -> status.is5xxServerError(), response -> {
                    log.error("[WORKER] Server error fetching metadata for ingestionId: {}, status: {}", 
                        ingestionId, response.statusCode());
                    return Mono.error(new RuntimeException("Server error: " + response.statusCode()));
                })
                .bodyToMono(IngestionMetadataDto.class)
                .block();

            log.info("[WORKER] Successfully fetched metadata for ingestionId: {}", ingestionId);
            return metadata;

        } catch (WebClientResponseException e) {
            log.error("[WORKER] HTTP error fetching metadata for ingestionId: {}, status: {}, message: {}", 
                ingestionId, e.getStatusCode(), e.getMessage(), e);
            throw new RuntimeException("Failed to fetch ingestion metadata: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[WORKER] Network or unexpected error fetching metadata for ingestionId: {}", ingestionId, e);
            throw new RuntimeException("Failed to fetch ingestion metadata", e);
        }
    }

    public void triggerProcessing(UUID ingestionId) {
        log.info("[WORKER] Triggering processing for ingestion: {}", ingestionId);

        try {
            webClient.post()
                .uri("/internal/ingestions/{ingestionId}/process", ingestionId)
                .header("X-Internal-Token", internalToken)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), response -> {
                    log.warn("[WORKER] Client error triggering processing for ingestionId: {}, status: {}", 
                        ingestionId, response.statusCode());
                    return Mono.error(new RuntimeException("Client error: " + response.statusCode()));
                })
                .onStatus(status -> status.is5xxServerError(), response -> {
                    log.error("[WORKER] Server error triggering processing for ingestionId: {}, status: {}", 
                        ingestionId, response.statusCode());
                    return Mono.error(new RuntimeException("Server error: " + response.statusCode()));
                })
                .bodyToMono(Void.class)
                .block();

            log.info("[WORKER] Processing triggered successfully for ingestion: {}", ingestionId);
        } catch (WebClientResponseException e) {
            log.error("[WORKER] HTTP error triggering processing for ingestionId: {}, status: {}, message: {}", 
                ingestionId, e.getStatusCode(), e.getMessage(), e);
            throw new RuntimeException("Failed to trigger processing: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[WORKER] Network or unexpected error triggering processing for ingestionId: {}", ingestionId, e);
            throw new RuntimeException("Failed to trigger processing", e);
        }
    }
}

