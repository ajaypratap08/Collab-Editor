package com.collab.editor.service;

import com.collab.editor.dto.OperationAck;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // ✅ message.getBody() is raw bytes from Redis
            // Jackson2JsonRedisSerializer stored it as JSON object bytes
            // So we deserialize directly from bytes — no double unwrapping
            OperationAck ack = objectMapper.readValue(
                    message.getBody(), OperationAck.class
            );

            log.debug("Received op from Redis for doc '{}', forwarding via WebSocket",
                    ack.getDocumentId());

            messagingTemplate.convertAndSend(
                    "/topic/document/" + ack.getDocumentId(), ack
            );

        } catch (Exception e) {
            log.error("Failed to process Redis message: {}", e.getMessage());
            log.error("Raw message body: {}", new String(message.getBody()));
        }
    }
}