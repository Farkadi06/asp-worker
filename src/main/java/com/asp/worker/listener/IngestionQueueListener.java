package com.asp.worker.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class IngestionQueueListener {

    public void handleMessage(String body) {
        log.info("[WORKER] Received message: {}", body);
    }
}

