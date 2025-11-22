package com.gideon.notes.dto;

public record ApiResponse(
        String message,
        Object data
) {
}
