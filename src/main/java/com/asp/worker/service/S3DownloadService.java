package com.asp.worker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3DownloadService {

    private final S3Client s3Client;

    @Value("${ASP_S3_BUCKET_NAME:}")
    private String bucketName;

    /**
     * Downloads a file from S3 and returns its bytes.
     *
     * @param path the S3 object key (storage path)
     * @return file bytes, or null if download fails
     */
    public byte[] downloadFile(String path) {
        if (bucketName == null || bucketName.trim().isEmpty()) {
            log.error("[S3] Cannot download file: ASP_S3_BUCKET_NAME is not configured");
            return null;
        }

        if (path == null || path.trim().isEmpty()) {
            log.error("[S3] Cannot download file: path is null or empty");
            return null;
        }

        try {
            log.debug("[S3] Downloading file from bucket: {}, path: {}", bucketName, path);

            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(path)
                .build();

            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(request);
            byte[] fileBytes = responseBytes.asByteArray();

            log.info("[S3] Successfully downloaded file: path={}, size={} bytes ({} KB)",
                path, fileBytes.length, fileBytes.length / 1024);

            return fileBytes;

        } catch (NoSuchKeyException e) {
            log.error("[S3] File not found in S3: bucket={}, path={}", bucketName, path, e);
            return null;
        } catch (S3Exception e) {
            log.error("[S3] S3 error downloading file: bucket={}, path={}, errorCode={}, errorMessage={}",
                bucketName, path, e.awsErrorDetails().errorCode(), e.awsErrorDetails().errorMessage(), e);
            return null;
        } catch (SdkException e) {
            log.error("[S3] SDK error downloading file: bucket={}, path={}", bucketName, path, e);
            return null;
        } catch (Exception e) {
            log.error("[S3] Unexpected error downloading file: bucket={}, path={}", bucketName, path, e);
            return null;
        }
    }

    /**
     * Downloads a file from S3 and saves it to a temporary file.
     *
     * @param ingestionId the ingestion ID (used for filename)
     * @param path the S3 object key (storage path)
     * @return Path to the temporary file, or null if download fails
     */
    public Path downloadToTemp(UUID ingestionId, String path) {
        if (ingestionId == null) {
            log.error("[S3] Cannot download to temp: ingestionId is null");
            return null;
        }

        if (path == null || path.trim().isEmpty()) {
            log.error("[S3] Cannot download to temp: path is null or empty");
            return null;
        }

        try {
            // Download file bytes
            byte[] fileBytes = downloadFile(path);
            if (fileBytes == null) {
                log.error("[S3] Failed to download file bytes for ingestionId: {}, path: {}", ingestionId, path);
                return null;
            }

            // Create temp directory if it doesn't exist
            Path tempDir = Paths.get("/tmp/ingestions");
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
                log.debug("[S3] Created temp directory: {}", tempDir);
            }

            // Save file to temp location
            Path tempFile = tempDir.resolve(ingestionId + ".pdf");
            Files.write(tempFile, fileBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            long fileSizeKB = fileBytes.length / 1024;
            log.info("[S3] Downloaded file to temp: ingestionId={}, size={} KB, saved to {}",
                ingestionId, fileSizeKB, tempFile);

            return tempFile;

        } catch (IOException e) {
            log.error("[S3] IO error saving temp file for ingestionId: {}, path: {}", ingestionId, path, e);
            return null;
        } catch (Exception e) {
            log.error("[S3] Unexpected error downloading to temp for ingestionId: {}, path: {}", ingestionId, path, e);
            return null;
        }
    }
}

