package com.collab.editor.service;

import com.collab.editor.dto.OperationMessage;
import com.collab.editor.model.Operation;
import org.springframework.stereotype.Component;

@Component
public class OTEngine {

    /**
     * Transform incoming op against all ops that were applied
     * since the client's baseVersion.
     *
     * @param incoming   the op sent by the client
     * @param concurrent ops applied to the doc since client's baseVersion
     * @return a new op with corrected position
     */
    public OperationMessage transform(
            OperationMessage incoming,
            java.util.List<Operation> concurrent) {

        int transformedPosition = incoming.getPosition();

        for (Operation applied : concurrent) {
            transformedPosition = transformPosition(
                    transformedPosition,
                    incoming.getType().name(),
                    applied
            );
        }

        // return a copy with the corrected position
        return new OperationMessage(
                incoming.getDocumentId(),
                incoming.getUserId(),
                incoming.getType(),
                transformedPosition,
                incoming.getText(),
                incoming.getLength(),
                incoming.getBaseVersion()
        );
    }

    /**
     * Core OT rule — adjust a position based on one already-applied op.
     *
     * Rule table:
     *  applied=INSERT before incoming → shift incoming right by inserted length
     *  applied=INSERT at same pos     → tie-break: applied wins, shift incoming right
     *  applied=INSERT after incoming  → no change
     *  applied=DELETE before incoming → shift incoming left by deleted length
     *  applied=DELETE overlaps        → clamp incoming to deletion start
     *  applied=DELETE after incoming  → no change
     */
    private int transformPosition(
            int position,
            String incomingType,
            Operation applied) {

        if (applied.getType() == Operation.OperationType.INSERT) {
            if (applied.getPosition() < position) {
                // applied insert shifted everything after it to the right
                return position + applied.getText().length();
            } else if (applied.getPosition() == position) {
                // tie-break: the op already applied wins → push incoming right
                return position + applied.getText().length();
            }
            // applied insert was after this position — no effect
            return position;
        }

        if (applied.getType() == Operation.OperationType.DELETE) {
            if (applied.getPosition() + applied.getLength() <= position) {
                // deletion was entirely before this position — shift left
                return position - applied.getLength();
            } else if (applied.getPosition() <= position) {
                // deletion overlaps this position — clamp to deletion start
                return applied.getPosition();
            }
            // deletion was after this position — no effect
            return position;
        }

        return position;
    }

    /**
     * Apply an operation to document content and return new content.
     */
    public String applyOperation(String content, OperationMessage op) {
        int pos = op.getPosition();

        return switch (op.getType()) {
            case INSERT -> {
                // guard against out-of-bounds positions
                int safePos = Math.min(pos, content.length());
                yield content.substring(0, safePos)
                        + op.getText()
                        + content.substring(safePos);
            }
            case DELETE -> {
                int safeStart = Math.min(pos, content.length());
                int safeEnd   = Math.min(pos + op.getLength(), content.length());
                yield content.substring(0, safeStart)
                        + content.substring(safeEnd);
            }
        };
    }
}