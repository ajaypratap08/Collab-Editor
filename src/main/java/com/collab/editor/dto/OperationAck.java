package com.collab.editor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// What the server broadcasts back after applying an operation
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OperationAck {

    private String operationId;      // id of the saved operation
    private String documentId;
    private String userId;           // who made the change
    private String type;
    private int position;            // transformed position (OT result)
    private String text;
    private int length;
    private int newVersion;          // document version after this op
    private boolean success;
    private String errorMessage;     // set only if success = false
}