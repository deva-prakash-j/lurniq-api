package com.lurniq.service;

import com.lurniq.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResponseService {
    
    private final RequestTrackingService requestTrackingService;
    
    public <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.success(
                message, 
                data, 
                requestTrackingService.getTimeTaken(), 
                requestTrackingService.getTraceId()
        );
    }
    
    public <T> ApiResponse<T> success(T data) {
        return success("Operation completed successfully", data);
    }
    
    public <T> ApiResponse<T> error(String message) {
        return ApiResponse.error(message, requestTrackingService.getTraceId());
    }
}
