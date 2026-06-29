package com.collab.editor.controller;

import com.collab.editor.dto.AuthRequest;
import com.collab.editor.dto.AuthResponse;
import com.collab.editor.dto.RefreshRequest;
import com.collab.editor.model.User;
import com.collab.editor.repository.UserRepository;
import com.collab.editor.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody AuthRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "An account with this email already exists"));
        }

        String refreshToken = jwtUtil.generateRefreshToken(request.getEmail());

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .refreshTokenHash(passwordEncoder.encode(refreshToken))
                .refreshTokenExpiry(Instant.now().plus(7, ChronoUnit.DAYS))
                .createdAt(Instant.now())
                .build();

        userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(user.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                AuthResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .email(user.getEmail())
                        .expiresInSeconds(jwtUtil.getAccessTokenExpirySeconds())
                        .build()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // WHY: identical message for "no such user" and "wrong password" —
            // never reveal which one it was, that lets attackers enumerate valid emails.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password"));
        }

        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        user.setRefreshTokenHash(passwordEncoder.encode(refreshToken));
        user.setRefreshTokenExpiry(Instant.now().plus(7, ChronoUnit.DAYS));
        userRepository.save(user);

        String accessToken = jwtUtil.generateAccessToken(user.getEmail());

        return ResponseEntity.ok(
                AuthResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .email(user.getEmail())
                        .expiresInSeconds(jwtUtil.getAccessTokenExpirySeconds())
                        .build()
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (user == null || user.getRefreshTokenHash() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid refresh token"));
        }

        boolean expired = user.getRefreshTokenExpiry() == null
                || Instant.now().isAfter(user.getRefreshTokenExpiry());

        boolean matches = passwordEncoder.matches(request.getRefreshToken(), user.getRefreshTokenHash());

        if (expired || !matches) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Refresh token expired or invalid, please log in again"));
        }

        // Rotate the refresh token on every use — limits damage if one is ever stolen
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        user.setRefreshTokenHash(passwordEncoder.encode(newRefreshToken));
        user.setRefreshTokenExpiry(Instant.now().plus(7, ChronoUnit.DAYS));
        userRepository.save(user);

        String newAccessToken = jwtUtil.generateAccessToken(user.getEmail());

        return ResponseEntity.ok(
                AuthResponse.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(newRefreshToken)
                        .email(user.getEmail())
                        .expiresInSeconds(jwtUtil.getAccessTokenExpirySeconds())
                        .build()
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setRefreshTokenHash(null);
            user.setRefreshTokenExpiry(null);
            userRepository.save(user);
        });
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
