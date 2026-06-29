package com.collab.editor.service;

import com.collab.editor.dto.OperationMessage;
import com.collab.editor.model.Document;
import com.collab.editor.model.Operation;
import com.collab.editor.repository.DocumentRepository;
import com.collab.editor.repository.OperationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final OperationRepository operationRepository;
    private final OTEngine otEngine;

    public Document createDocument(String title, String ownerId) {
        Document doc = Document.builder()
                .title(title)
                .content("")
                .ownerId(ownerId)
                .version(0)
                .build();
        return documentRepository.save(doc);
    }

    public Document getDocument(String documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
    }

    /**
     * List all documents owned by a specific user — this is what the
     * dashboard calls to show "your documents". Scoped by ownerId rather
     * than returning every document in the database, since the frontend
     * will call this per logged-in user.
     */
    public List<Document> getDocumentsForOwner(String ownerId) {
        return documentRepository.findByOwnerId(ownerId);
    }

    /**
     * Core method — called every time a user sends an operation.
     * 1. Fetch concurrent ops (applied since client's baseVersion)
     * 2. OT-transform the incoming op
     * 3. Apply to document content
     * 4. Save op to history
     * 5. Return transformed op so it can be broadcast
     */
    public OperationMessage applyOperation(OperationMessage incoming) {
        Document doc = getDocument(incoming.getDocumentId());

        // get all ops applied after the client's base version
        List<Operation> concurrent = operationRepository
                .findByDocumentIdAndDocumentVersionGreaterThanEqual(
                        incoming.getDocumentId(),
                        incoming.getBaseVersion()
                );

        // transform position against concurrent ops
        OperationMessage transformed = otEngine.transform(incoming, concurrent);

        // apply to current document content
        String newContent = otEngine.applyOperation(doc.getContent(), transformed);

        // update document
        doc.setContent(newContent);
        doc.setVersion(doc.getVersion() + 1);
        documentRepository.save(doc);

        // persist operation to history
        Operation operation = Operation.builder()
                .documentId(doc.getId())
                .userId(incoming.getUserId())
                .type(Operation.OperationType.valueOf(transformed.getType().name()))
                .position(transformed.getPosition())
                .text(transformed.getText())
                .length(transformed.getLength())
                .documentVersion(doc.getVersion())
                .timestamp(Instant.now())
                .build();
        operationRepository.save(operation);

        log.debug("Applied op by {} on doc {} → version {}",
                incoming.getUserId(), doc.getId(), doc.getVersion());

        return transformed;
    }

    public List<Operation> getHistory(String documentId) {
        return operationRepository
                .findByDocumentIdOrderByTimestampAsc(documentId);
    }
    /**
     * Rollback document to a specific version.
     * Strategy: replay all operations up to and including targetVersion
     * starting from empty content.
     *
     * Why replay from scratch instead of "undoing" recent ops?
     * Because undoing is complex — you'd need inverse operations.
     * Replaying forward from zero is simple, correct, and fast enough
     * for documents under ~10,000 operations.
     */
    public Document rollbackToVersion(String documentId, int targetVersion) {
        Document doc = getDocument(documentId);

        if (targetVersion < 0) {
            throw new RuntimeException("Target version cannot be negative");
        }
        if (targetVersion >= doc.getVersion()) {
            throw new RuntimeException(
                    "Target version " + targetVersion +
                            " must be less than current version " + doc.getVersion()
            );
        }

        // fetch all ops for this document in chronological order
        List<Operation> allOps = operationRepository
                .findByDocumentIdOrderByTimestampAsc(documentId);

        log.debug("Rollback doc {} from version {} to version {} — replaying {} ops",
                documentId, doc.getVersion(), targetVersion,
                Math.min(targetVersion, allOps.size()));

        // replay ops from scratch up to targetVersion
        String rebuiltContent = "";
        for (int i = 0; i < targetVersion && i < allOps.size(); i++) {
            Operation op = allOps.get(i);
            OperationMessage msg = new OperationMessage(
                    documentId,
                    op.getUserId(),
                    op.getType(),
                    op.getPosition(),
                    op.getText(),
                    op.getLength(),
                    i
            );
            rebuiltContent = otEngine.applyOperation(rebuiltContent, msg);
        }

        // save the rolled-back state
        doc.setContent(rebuiltContent);
        doc.setVersion(targetVersion);
        documentRepository.save(doc);

        // delete ops after the target version — they no longer apply
        List<Operation> opsToDelete = allOps.subList(
                Math.min(targetVersion, allOps.size()),
                allOps.size()
        );
        operationRepository.deleteAll(opsToDelete);

        log.info("Rolled back doc {} to version {} — content: '{}'",
                documentId, targetVersion, rebuiltContent);

        return doc;
    }

    /**
     * Get a read-only snapshot of what the document looked like
     * at a specific version — without actually rolling back.
     * Useful for previewing history before committing to a rollback.
     */
    public Document previewVersion(String documentId, int targetVersion) {
        List<Operation> allOps = operationRepository
                .findByDocumentIdOrderByTimestampAsc(documentId);

        String rebuiltContent = "";
        for (int i = 0; i < targetVersion && i < allOps.size(); i++) {
            Operation op = allOps.get(i);
            OperationMessage msg = new OperationMessage(
                    documentId,
                    op.getUserId(),
                    op.getType(),
                    op.getPosition(),
                    op.getText(),
                    op.getLength(),
                    i
            );
            rebuiltContent = otEngine.applyOperation(rebuiltContent, msg);
        }

        // return a non-persisted snapshot — don't save this
        Document snapshot = getDocument(documentId);
        snapshot.setContent(rebuiltContent);
        snapshot.setVersion(targetVersion);
        return snapshot;
    }
}