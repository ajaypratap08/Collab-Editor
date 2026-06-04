package com.collab.editor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker          // turns on STOMP message broker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // Prefix for messages the SERVER sends TO clients
        // Client subscribes to: /topic/document/{id}
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix for messages the CLIENT sends TO server
        // Client sends to: /app/document/{id}/operation
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific messages (presence, errors)
        // /user/{userId}/queue/errors
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        // The URL clients connect to via WebSocket
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // tighten this in production
                .withSockJS();                  // fallback for browsers without WS support
    }
}