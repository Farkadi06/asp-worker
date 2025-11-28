package com.asp.worker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@Slf4j
public class SqsListenerConfig {

    public static final String QUEUE_URL_ENV = "ASP_SQS_INGESTION_QUEUE_URL";

    @Value("#{systemEnvironment['ASP_SQS_INGESTION_QUEUE_URL']}")
    private String queueUrl;

    @Value("${AWS_REGION:us-east-1}")
    private String awsRegion;

    @PostConstruct
    void logQueue() {
        if (queueUrl == null || queueUrl.isBlank()) {
            throw new IllegalStateException("Environment variable " + QUEUE_URL_ENV + " must be set");
        }
        log.info("[WORKER] Listening on SQS queue: {}", queueUrl);
    }

    @Bean
    public SqsClient sqsClient() {
        log.info("[WORKER] Creating SqsClient in region {}", awsRegion);
        return SqsClient.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }

    @Bean(name = "ingestionQueueUrl")
    public String ingestionQueueUrl() {
        if (queueUrl == null || queueUrl.isBlank()) {
            throw new IllegalStateException("Environment variable " + QUEUE_URL_ENV + " must be set");
        }
        return queueUrl;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}

