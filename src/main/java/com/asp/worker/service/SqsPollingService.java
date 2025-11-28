package com.asp.worker.service;

import com.asp.worker.listener.IngestionQueueListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SqsPollingService {

    private final SqsClient sqsClient;
    private final IngestionQueueListener ingestionQueueListener;

    @Value("${asp.sqs.queue-url}")
    private String queueUrl;

    @Scheduled(fixedDelay = 5000)
    public void poll() {
        try {
            log.debug("[WORKER] Polling SQS...");

            ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .waitTimeSeconds(10)
                    .maxNumberOfMessages(5)
                    .build();

            ReceiveMessageResponse response = sqsClient.receiveMessage(request);
            List<Message> messages = response.messages();

            if (messages == null || messages.isEmpty()) {
                log.debug("[WORKER] No messages on queue.");
                return;
            }

            log.info("[WORKER] Received {} message(s) from SQS", messages.size());

            for (Message msg : messages) {
                log.info("[WORKER] Received SQS message {} ({} bytes)", msg.messageId(), msg.body() != null ? msg.body().length() : 0);

                try {
                    ingestionQueueListener.handleMessage(msg.messageId(), msg.body());

                    sqsClient.deleteMessage(DeleteMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(msg.receiptHandle())
                            .build());

                    log.info("[WORKER] Deleted message {}", msg.messageId());
                } catch (Exception e) {
                    log.error("[WORKER] Error processing message {}", msg.messageId(), e);
                    // Continue processing other messages even if one fails
                }
            }
        } catch (Exception e) {
            log.error("[WORKER] Error while polling SQS queue", e);
        }
    }
}

