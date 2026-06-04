package com.collab.editor.dto;

import lombok.Builder;
import lombok.Data;

// Broadcast when a user joins or leaves a document
@Data
@Builder
public class PresenceMessage {

    private String documentId;
    private String userId;
    private String username;
    private PresenceEvent event;
    private int cursorPosition;      // where their cursor is

    public enum PresenceEvent {
        JOINED, LEFT, CURSOR_MOVED
    }
}