package com.asp.worker.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = false)
public class IngestionQueueMessage {

    @NotNull
    @JsonProperty(value = "version", required = true)
    private String version;

    @NotNull
    @JsonProperty(value = "ingestionId", required = true)
    private UUID ingestionId;

    @NotNull
    @JsonProperty(value = "tenantId", required = true)
    private UUID tenantId;

    @NotNull
    @JsonProperty(value = "filePath", required = true)
    private String filePath;

    @NotNull
    @JsonProperty(value = "createdAt", required = true)
    private Instant createdAt;

    @NotNull
    @JsonProperty(value = "traceId", required = true)
    private String traceId;

    @NotNull
    @JsonProperty(value = "attempt", required = true)
    private Integer attempt;
}

