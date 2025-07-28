package com.lurniq.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class RequestTrackingService {
    
    private static final String TRACE_ID_KEY = "traceId";
    private static final String SPAN_ID_KEY = "spanId";
    private static final ThreadLocal<Long> REQUEST_START_TIME = new ThreadLocal<>();
    
    public void startRequest() {
        REQUEST_START_TIME.set(System.currentTimeMillis());
        
        // Generate trace ID if not already present in MDC
        String traceId = MDC.get(TRACE_ID_KEY);
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
            MDC.put(TRACE_ID_KEY, traceId);
        }
        
        // Generate span ID
        String spanId = generateSpanId();
        MDC.put(SPAN_ID_KEY, spanId);
        
        log.debug("Request started with trace ID: {}, span ID: {}", traceId, spanId);
    }
    
    public void endRequest() {
        String traceId = getTraceId();
        String spanId = getSpanId();
        
        REQUEST_START_TIME.remove();
        MDC.remove(SPAN_ID_KEY);
        // Keep traceId in MDC for the duration of the request
        
        log.debug("Request ended with trace ID: {}, span ID: {}", traceId, spanId);
    }
    
    public Long getTimeTaken() {
        Long startTime = REQUEST_START_TIME.get();
        return startTime != null ? System.currentTimeMillis() - startTime : null;
    }
    
    public String getTraceId() {
        String traceId = MDC.get(TRACE_ID_KEY);
        if (traceId == null || traceId.isEmpty()) {
            // Fallback: generate a trace ID if none exists
            traceId = generateTraceId();
            MDC.put(TRACE_ID_KEY, traceId);
        }
        return traceId;
    }
    
    public String getSpanId() {
        return MDC.get(SPAN_ID_KEY);
    }
    
    private String generateTraceId() {
        // Generate a 32-character hex string (128-bit) for OpenTelemetry compatibility
        return UUID.randomUUID().toString().replace("-", "") + 
               UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    private String generateSpanId() {
        // Generate a 16-character hex string (64-bit) for OpenTelemetry compatibility
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    public void clearTraceContext() {
        MDC.remove(TRACE_ID_KEY);
        MDC.remove(SPAN_ID_KEY);
    }
}
