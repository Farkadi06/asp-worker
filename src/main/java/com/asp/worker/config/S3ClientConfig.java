package com.asp.worker.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@Slf4j
public class S3ClientConfig {

    @Value("${AWS_REGION:us-east-1}")
    private String awsRegion;

    @Value("${ASP_S3_BUCKET_NAME:}")
    private String bucketName;

    @PostConstruct
    public void validateConfiguration() {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            log.error("⚠️  ASP_S3_BUCKET_NAME is not configured! S3 operations will fail.");
            log.error("   Please set ASP_S3_BUCKET_NAME environment variable.");
        } else {
            log.info("S3 bucket configured: {}", bucketName);
        }
        log.info("AWS region configured: {}", awsRegion);
    }

    @Bean
    public S3Client s3Client() {
        Region region = Region.of(awsRegion);
        log.info("Initializing S3Client with region: {}", region);
        
        S3Client client = S3Client.builder()
            .region(region)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
        
        log.info("S3Client initialized successfully");
        return client;
    }
}

