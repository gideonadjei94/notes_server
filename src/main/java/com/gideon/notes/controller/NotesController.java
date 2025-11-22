package com.gideon.notes.controller;

import com.gideon.notes.dto.NoteDto;
import com.gideon.notes.service.notes.NoteServiceInt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@Tag(name = "Note", description = "Note management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class NotesController {

    private final NoteServiceInt noteService;

    @PostMapping
    @Operation(
            summary = "Create a new note",
            description = "Create a new note for the authenticated user",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Note created successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = NoteDto.NoteResponse.class),
                                    examples = @ExampleObject(value = """
                                            {
                                              "id": 1,
                                              "title": "My First Note",
                                              "content": "This is the content of my note",
                                              "tags": ["work", "important"],
                                              "version": 0,
                                              "createdAt": "2024-01-15T10:30:00",
                                              "updatedAt": "2024-01-15T10:30:00",
                                              "deletedAt": null
                                            }
                                            """)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation error",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
                    )
            }
    )
    public ResponseEntity<NoteDto.NoteResponse> createNote(
            @Valid @RequestBody NoteDto.NoteRequest request,
            Authentication authentication) {
        NoteDto.NoteResponse response = noteService.createNote(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }



    @GetMapping
    @Operation(
            summary = "Get all notes",
            description = "Retrieve paginated list of notes with optional search and tag filtering",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Note retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = NoteDto.PagedNotesResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<NoteDto.PagedNotesResponse> getNotes(
            @Parameter(description = "Search query for title/content")
            @RequestParam(required = false) String search,

            @Parameter(description = "Filter by tag (comma-separated for multiple)")
            @RequestParam(required = false) String tag,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Sort by field (default: updatedAt)")
            @RequestParam(required = false, defaultValue = "updatedAt") String sortBy,
            Authentication authentication) {
        NoteDto.PagedNotesResponse response = noteService.getNotes(
                authentication.getName(), search, tag, page, size, sortBy);
        return ResponseEntity.ok(response);
    }



    @GetMapping("/{id}")
    @Operation(
            summary = "Get note by ID",
            description = "Retrieve a specific note by its ID",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Note found",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = NoteDto.NoteResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Note not found",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
                    )
            }
    )
    public ResponseEntity<NoteDto.NoteResponse> getNoteById(
            @PathVariable Long id,
            Authentication authentication) {
        NoteDto.NoteResponse response = noteService.getNoteById(authentication.getName(), id);
        return ResponseEntity.ok()
                .eTag(String.valueOf(response.getVersion()))
                .body(response);
    }



    @PutMapping("/{id}")
    @Operation(
            summary = "Update a note",
            description = "Update an existing note. Supports optimistic locking via If-Match header.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Note updated successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = NoteDto.NoteResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Note not found",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Version conflict - note was modified by another user",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
                    )
            }
    )
    public ResponseEntity<NoteDto.NoteResponse> updateNote(
            @PathVariable Long id,
            @Valid @RequestBody NoteDto.NoteRequest request,
            @Parameter(description = "Expected version for optimistic locking")
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            Authentication authentication) {

        Long version = null;
        if (ifMatch != null && !ifMatch.isEmpty()) {
            // Remove quotes if present
            String versionStr = ifMatch.replace("\"", "");
            try {
                version = Long.parseLong(versionStr);
            } catch (NumberFormatException e) {
                // Invalid version format, will be handled by service
            }
        }

        NoteDto.NoteResponse response = noteService.updateNote(
                authentication.getName(), id, request, version);
        return ResponseEntity.ok()
                .eTag(String.valueOf(response.getVersion()))
                .body(response);
    }



    @DeleteMapping("/{id}")
    @Operation(
            summary = "Soft delete a note",
            description = "Soft delete a note (sets deletedAt timestamp)",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Note deleted successfully"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Note not found",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
                    )
            }
    )
    public ResponseEntity<Void> deleteNote(
            @PathVariable Long id,
            Authentication authentication) {
        noteService.deleteNote(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }



    @PostMapping("/{id}/restore")
    @Operation(
            summary = "Restore a deleted note",
            description = "Restore a soft-deleted note",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Note restored successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = NoteDto.NoteResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Deleted note not found",
                            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
                    )
            }
    )
    public ResponseEntity<NoteDto.NoteResponse> restoreNote(
            @PathVariable Long id,
            Authentication authentication) {
        NoteDto.NoteResponse response = noteService.restoreNote(authentication.getName(), id);
        return ResponseEntity.ok(response);
    }
}