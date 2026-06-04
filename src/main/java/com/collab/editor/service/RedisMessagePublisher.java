package com.collab.editor.service;

import com.collab.editor.dto.OperationAck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    // ✅ ObjectMapper REMOVED — we don't serialize manually anymore

    public static String channelFor(String documentId) {
        return "document:" + documentId + ":operations";
    }

    public void publishOperation(String documentId, OperationAck ack) {
        String channel = channelFor(documentId);

        // ✅ Pass the object directly — RedisTemplate serializes it once
        redisTemplate.convertAndSend(channel, ack);

        log.debug("Published op to Redis channel '{}' by user '{}'",
                channel, ack.getUserId());
    }
}