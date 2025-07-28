package com.lurniq.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    @Builder.Default
    private String status = "error";
    
    private String message;
    private String error;
    private String path;
    private Long timeTaken;
    private String traceId;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime timestamp;
    
    // Field-specific validation errors
    private Map<String, String> fieldErrors;
    
    public static ErrorResponse of(int statusCode, String error, String message, String path) {
        return ErrorResponse.builder()
                .status("error")
                .error(error)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ErrorResponse of(int statusCode, String error, String message, String path, String traceId) {
        return ErrorResponse.builder()
                .status("error")
                .error(error)
                .message(message)
                .path(path)
                .traceId(traceId)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ErrorResponse of(int statusCode, String error, String message, String path, String traceId, Long timeTaken) {
        return ErrorResponse.builder()
                .status("error")
                .error(error)
                .message(message)
                .path(path)
                .traceId(traceId)
                .timeTaken(timeTaken)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ErrorResponse withFieldErrors(int statusCode, String error, String message, String path, String traceId, Long timeTaken, Map<String, String> fieldErrors) {
        return ErrorResponse.builder()
                .status("error")
                .error(error)
                .message(message)
                .path(path)
                .traceId(traceId)
                .timeTaken(timeTaken)
                .fieldErrors(fieldErrors)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
