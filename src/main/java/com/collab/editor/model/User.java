package com.collab.editor.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    // BCrypt-hashed, never the raw password
    private String password;

    // Hashed refresh token (we never store it in plaintext, same reason we hash passwords)
    private String refreshTokenHash;

    private Instant refreshTokenExpiry;

    private Instant createdAt;
}
