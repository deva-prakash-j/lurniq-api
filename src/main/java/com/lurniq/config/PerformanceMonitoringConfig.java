package com.lurniq.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Performance monitoring configuration with custom metrics
 */
@Configuration
public class PerformanceMonitoringConfig {

    @Bean
    public PerformanceMonitoringFilter performanceMonitoringFilter(MeterRegistry meterRegistry) {
        return new PerformanceMonitoringFilter(meterRegistry);
    }

    @Slf4j
    public static class PerformanceMonitoringFilter extends OncePerRequestFilter {
        
        private final Timer requestTimer;
        private final Counter requestCounter;
        private final Counter slowRequestCounter;
        private final Counter errorCounter;
        
        private static final long SLOW_REQUEST_THRESHOLD_MS = 1000;
        
        public PerformanceMonitoringFilter(MeterRegistry meterRegistry) {
            this.requestTimer = Timer.builder("http.requests")
                .description("HTTP request duration")
                .register(meterRegistry);
                
            this.requestCounter = Counter.builder("http.requests.total")
                .description("Total HTTP requests")
                .register(meterRegistry);
                
            this.slowRequestCounter = Counter.builder("http.requests.slow")
                .description("Slow HTTP requests (>1s)")
                .register(meterRegistry);
                
            this.errorCounter = Counter.builder("http.requests.errors")
                .description("HTTP request errors")
                .register(meterRegistry);
        }
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {
            
            Timer.Sample sample = Timer.start();
            
            try {
                filterChain.doFilter(request, response);
                requestCounter.increment();
                
                long durationMs = sample.stop(requestTimer);
                if (durationMs > SLOW_REQUEST_THRESHOLD_MS * 1000000) { // Convert to nanoseconds
                    slowRequestCounter.increment();
                    log.warn("Slow request detected: {} {} took {}ms", 
                        request.getMethod(), request.getRequestURI(), durationMs / 1000000);
                }
                
                if (response.getStatus() >= 400) {
                    errorCounter.increment();
                }
                
            } catch (Exception e) {
                errorCounter.increment();
                throw e;
            }
        }
    }
}
