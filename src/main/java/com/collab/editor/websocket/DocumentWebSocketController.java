package com.collab.editor.websocket;

import com.collab.editor.dto.OperationAck;
import com.collab.editor.dto.OperationMessage;
import com.collab.editor.dto.PresenceMessage;
import com.collab.editor.service.DocumentService;
import com.collab.editor.service.RedisMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DocumentWebSocketController {

    private final DocumentService documentService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedisMessagePublisher redisPublisher;   // ← ADDED

    @MessageMapping("/document/{docId}/operation")
    public void handleOperation(
            @DestinationVariable String docId,
            @Payload OperationMessage message) {

        log.debug("Received op from user '{}' on doc '{}'",
                message.getUserId(), docId);

        try {
            OperationMessage transformed = documentService.applyOperation(message);

            OperationAck ack = OperationAck.builder()
                    .documentId(docId)
                    .userId(message.getUserId())
                    .type(transformed.getType().name())
                    .position(transformed.getPosition())
                    .text(transformed.getText())
                    .length(transformed.getLength())
                    .newVersion(documentService.getDocument(docId).getVersion())
                    .success(true)
                    .build();

            // ✅ Publish to Redis instead of directly to WebSocket
            // Redis subscriber will forward to WebSocket — works across all nodes
            redisPublisher.publishOperation(docId, ack);

        } catch (Exception e) {
            log.error("Failed to apply operation: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                    message.getUserId(),
                    "/queue/errors",
                    OperationAck.builder()
                            .documentId(docId)
                            .success(false)
                            .errorMessage(e.getMessage())
                            .build()
            );
        }
    }

    @MessageMapping("/document/{docId}/presence")
    public void handlePresence(
            @DestinationVariable String docId,
            @Payload PresenceMessage message) {

        log.debug("Presence: user '{}' event '{}' on doc '{}'",
                message.getUserId(), message.getEvent(), docId);

        messagingTemplate.convertAndSend(
                "/topic/document/" + docId + "/presence", message
        );
    }
}