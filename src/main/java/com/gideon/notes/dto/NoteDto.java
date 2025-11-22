package com.gideon.notes.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class NoteDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Request to create or update a note")
    public static class NoteRequest {

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        @Schema(description = "Note title", example = "My Important Note")
        private String title;

        @NotBlank(message = "Content is required")
        @Schema(description = "Note content", example = "This is the content of my note with important information.")
        private String content;

        @Schema(description = "Tags associated with the note", example = "[\"work\", \"important\", \"todo\"]")
        private List<String> tags;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Note response")
    public static class NoteResponse {

        @Schema(description = "Note ID", example = "1")
        private Long id;

        @Schema(description = "Note title", example = "My Important Note")
        private String title;

        @Schema(description = "Note content", example = "This is the content of my note.")
        private String content;

        @Schema(description = "Tags", example = "[\"work\", \"important\"]")
        private List<String> tags;

        @Schema(description = "Version for optimistic locking", example = "0")
        private Long version;

        @Schema(description = "Creation timestamp", example = "2024-01-15T10:30:00")
        private LocalDateTime createdAt;

        @Schema(description = "Last update timestamp", example = "2024-01-15T14:20:00")
        private LocalDateTime updatedAt;

        @Schema(description = "Deletion timestamp (null if not deleted)", example = "null")
        private LocalDateTime deletedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Paginated response for notes")
    public static class PagedNotesResponse {

        @Schema(description = "List of notes")
        private List<NoteResponse> notes;

        @Schema(description = "Current page number", example = "0")
        private int page;

        @Schema(description = "Page size", example = "10")
        private int size;

        @Schema(description = "Total number of elements", example = "45")
        private long totalElements;

        @Schema(description = "Total number of pages", example = "5")
        private int totalPages;

        @Schema(description = "Is this the last page", example = "false")
        private boolean last;
    }
}
