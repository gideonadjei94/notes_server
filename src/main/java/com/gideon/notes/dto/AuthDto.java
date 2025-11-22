package com.gideon.notes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDto {
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "User registration request")
    public static class SignupRequest {

        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Schema(description = "Username", example = "johndoe")
        private String username;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Schema(description = "Email address", example = "john@example.com")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        @Schema(description = "Password", example = "password123")
        private String password;
    }



    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "User login request")
    public static class LoginRequest {

        @NotBlank(message = "Username is required")
        @Schema(description = "Username", example = "johndoe")
        private String username;

        @NotBlank(message = "Password is required")
        @Schema(description = "Password", example = "password123")
        private String password;
    }



    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Authentication response with JWT token")
    public static class AuthResponse {

        @Schema(description = "User ID", example = "1")
        private Long userId;

        @Schema(description = "Username", example = "johndoe")
        private String username;

        @Schema(description = "Email address", example = "john@example.com")
        private String email;

        @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private String token;

        @Schema(description = "JWT refresh token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        private String refresh_token;

    }
}
