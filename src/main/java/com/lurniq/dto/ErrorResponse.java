package com.lurniq.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    private boolean success;
    private String error;
    private String message;
    private String path;
    private int status;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    public static ErrorResponse of(int status, String error, String message, String path) {
        return ErrorResponse.builder()
                .success(false)
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
