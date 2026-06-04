package com.collab.editor.controller;

import com.collab.editor.dto.OperationHistoryEntry;
import com.collab.editor.model.Document;
import com.collab.editor.model.Operation;
import com.collab.editor.service.DocumentService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    // POST /api/documents
    // Body: { "title": "My Doc", "ownerId": "user123" }
    @PostMapping
    public ResponseEntity<Document> createDocument(
            @RequestBody Map<String, String> body) {

        String title   = body.get("title");
        String ownerId = body.get("ownerId");

        if (title == null || ownerId == null) {
            return ResponseEntity.badRequest().build();
        }

        Document created = documentService.createDocument(title, ownerId);
        log.info("Created document '{}' for owner '{}'", title, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // GET /api/documents/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(@PathVariable String id) {
        try {
            Document doc = documentService.getDocument(id);
            return ResponseEntity.ok(doc);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // GET /api/documents/{id}/history
    // GET /api/documents/{id}/history
// Returns full operation log with human-readable summaries
    @GetMapping("/{id}/history")
    public ResponseEntity<List<OperationHistoryEntry>> getHistory(
            @PathVariable String id) {
        try {
            List<Operation> ops = documentService.getHistory(id);
            List<OperationHistoryEntry> history = ops.stream()
                    .map(OperationHistoryEntry::from)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(history);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // GET /api/documents/{id}/preview/{version}
// See what the document looked like at a version — without rolling back
    @GetMapping("/{id}/preview/{version}")
    public ResponseEntity<Document> previewVersion(
            @PathVariable String id,
            @PathVariable int version) {
        try {
            Document snapshot = documentService.previewVersion(id, version);
            return ResponseEntity.ok(snapshot);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // POST /api/documents/{id}/rollback/{version}
// Actually roll back — destructive, deletes ops after target version
    @PostMapping("/{id}/rollback/{version}")
    public ResponseEntity<Document> rollback(
            @PathVariable String id,
            @PathVariable int version) {
        try {
            Document rolled = documentService.rollbackToVersion(id, version);
            return ResponseEntity.ok(rolled);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(null);
        }
    }

    // GET /api/documents/{id}/version
    // Lightweight check — just returns current version number
    @GetMapping("/{id}/version")
    public ResponseEntity<Map<String, Object>> getVersion(@PathVariable String id) {
        try {
            Document doc = documentService.getDocument(id);
            return ResponseEntity.ok(Map.of(
                    "documentId", doc.getId(),
                    "version",    doc.getVersion(),
                    "title",      doc.getTitle()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}