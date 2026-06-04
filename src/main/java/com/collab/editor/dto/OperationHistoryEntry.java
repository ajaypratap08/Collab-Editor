package com.collab.editor.dto;

import com.collab.editor.model.Operation;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class OperationHistoryEntry {

    private String operationId;
    private int versionAfter;       // document version after this op applied
    private String userId;
    private String type;            // INSERT or DELETE
    private int position;
    private String text;            // what was inserted (null for DELETE)
    private int length;             // how many chars deleted (0 for INSERT)
    private String summary;         // human-readable: "user-ajay inserted 'Hello' at pos 0"
    private Instant timestamp;

    public static OperationHistoryEntry from(Operation op) {
        String summary = buildSummary(op);
        return OperationHistoryEntry.builder()
                .operationId(op.getId())
                .versionAfter(op.getDocumentVersion())
                .userId(op.getUserId())
                .type(op.getType().name())
                .position(op.getPosition())
                .text(op.getText())
                .length(op.getLength())
                .summary(summary)
                .timestamp(op.getTimestamp())
                .build();
    }

    private static String buildSummary(Operation op) {
        if (op.getType() == Operation.OperationType.INSERT) {
            String preview = op.getText() != null && op.getText().length() > 20
                    ? op.getText().substring(0, 20) + "..."
                    : op.getText();
            return op.getUserId() + " inserted '" + preview +
                    "' at position " + op.getPosition();
        } else {
            return op.getUserId() + " deleted " + op.getLength() +
                    " chars at position " + op.getPosition();
        }
    }
}