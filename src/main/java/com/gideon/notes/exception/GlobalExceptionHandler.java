package com.gideon.notes.exception;

import com.gideon.notes.dto.ApiResponse;
import jakarta.persistence.OptimisticLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse> handleEntityNotFoundException(EntityNotFoundException ex){
        return ResponseEntity
                .status(NOT_FOUND)
                .body(new ApiResponse(ex.getMessage(), null));
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ApiResponse> OptimisticLockException(OptimisticLockException  ex){
        return ResponseEntity
                .status(CONFLICT)
                .body(new ApiResponse(ex.getMessage(), null));
    }


    @ExceptionHandler( VersionConflictException.class)
    public ResponseEntity<ApiResponse> handleVersionException(VersionConflictException  ex){
        return ResponseEntity
                .status(CONFLICT)
                .body(new ApiResponse(ex.getMessage(), null));
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        ApiResponse response = new ApiResponse("Validation failed", errors);
        return ResponseEntity.status(BAD_REQUEST).body(response);
    }


    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse> handleBadCredentialsException(BadCredentialsException ex){
        return ResponseEntity
                .status(BAD_REQUEST)
                .body(new ApiResponse(ex.getMessage(), null));
    }


    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse> handleRateLimitException(RateLimitExceededException ex){
        return ResponseEntity
                .status(TOO_MANY_REQUESTS)
                .body(new ApiResponse(ex.getMessage(), null));
    }


    @ExceptionHandler(EntityAlreadyExists.class)
    public ResponseEntity<ApiResponse> handleEntityAlreadyExistsException(EntityAlreadyExists ex){
        return ResponseEntity
            .status(CONFLICT)
                .body(new ApiResponse(ex.getMessage(), null));
    }


    @ExceptionHandler(ExpiredAuthTokenException.class)
    public ResponseEntity<ApiResponse> handleExpiredAuthTokenException(ExpiredAuthTokenException ex){
        return ResponseEntity
                .status(UNAUTHORIZED)
                .body(new ApiResponse(ex.getMessage(), null));
    }


    @ExceptionHandler(InvalidAuthTokenException.class)
    public ResponseEntity<ApiResponse> handleInvalidTokenException(InvalidAuthTokenException ex){
        return ResponseEntity
                .status(UNAUTHORIZED)
                .body(new ApiResponse(ex.getMessage(), null));
    }


    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse> handleAuthenticationException(AuthenticationException ex){
        return ResponseEntity
                .status(UNAUTHORIZED)
                .body(new ApiResponse(ex.getMessage(), null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgException(IllegalArgumentException ex){
        return ResponseEntity
                .status(BAD_REQUEST)
                .body(new ApiResponse(ex.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleException(Exception ex){
        return ResponseEntity
                .status(INTERNAL_SERVER_ERROR)
                .body(new ApiResponse(ex.getMessage(), null));
    }
}