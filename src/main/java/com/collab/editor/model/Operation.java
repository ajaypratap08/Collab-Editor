package com.collab.editor.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "operations")
public class Operation {

    @Id
    private String id;

    private String documentId;     // which document this op belongs to

    private String userId;         // who made this change

    private OperationType type;    // INSERT or DELETE

    private int position;          // cursor position in the text

    private String text;           // text inserted (for INSERT ops)

    private int length;            // how many chars deleted (for DELETE ops)

    private int documentVersion;   // document version when this op was created

    private Instant timestamp;

    public enum OperationType {
        INSERT, DELETE
    }
}