package com.asp.worker.service;

import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class IdempotencyService {

    private final ConcurrentMap<String, Boolean> processedMessages = new ConcurrentHashMap<>();

    public boolean hasProcessed(String messageId) {
        String key = normalize(messageId);
        return key != null && processedMessages.containsKey(key);
    }

    public void markProcessed(String messageId) {
        String key = normalize(messageId);
        if (key != null) {
            processedMessages.put(key, Boolean.TRUE);
        }
    }

    private String normalize(String messageId) {
        if (messageId == null) {
            return null;
        }
        String trimmed = messageId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

