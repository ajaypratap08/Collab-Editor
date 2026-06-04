package com.collab.editor.dto;

import com.collab.editor.model.Operation.OperationType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// What a client sends when they make an edit
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationMessage {

    private String documentId;
    private String userId;
    private OperationType type;      // INSERT or DELETE
    private int position;            // cursor position
    private String text;             // text to insert (null for DELETE)
    private int length;              // chars to delete (0 for INSERT)
    private int baseVersion;         // document version client was on when editing
}