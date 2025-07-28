package com.lurniq.interceptor;

import com.lurniq.service.RequestTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestTrackingInterceptor implements HandlerInterceptor {
    
    private final RequestTrackingService requestTrackingService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        requestTrackingService.startRequest();
        
        String traceId = requestTrackingService.getTraceId();
        String spanId = requestTrackingService.getSpanId();
        
        // Add trace information to response headers
        response.setHeader("X-Trace-ID", traceId);
        response.setHeader("X-Span-ID", spanId);
        
        log.info("Request started - Method: {}, URI: {}, TraceID: {}, SpanID: {}", 
                request.getMethod(), request.getRequestURI(), traceId, spanId);
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long timeTaken = requestTrackingService.getTimeTaken();
        String traceId = requestTrackingService.getTraceId();
        String spanId = requestTrackingService.getSpanId();
        
        log.info("Request completed - Method: {}, URI: {}, Status: {}, Time: {}ms, TraceID: {}, SpanID: {}", 
                request.getMethod(), request.getRequestURI(), response.getStatus(), timeTaken, traceId, spanId);
        
        requestTrackingService.endRequest();
        // Clean up trace context after request completion
        requestTrackingService.clearTraceContext();
    }
}
