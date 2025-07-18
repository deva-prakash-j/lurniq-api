package com.lurniq.config;

import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration to handle Docker/Railway container metrics issues
 * Disables problematic system metrics that cause cgroup-related errors
 */
@Configuration
public class MetricsConfig {

    /**
     * Disable ProcessorMetrics to avoid cgroup issues in containers
     * This prevents the java.lang.NullPointerException: Cannot invoke "jdk.internal.platform.CgroupInfo.getMountPoint()"
     */
    @Bean
    @Primary
    @ConditionalOnProperty(value = "management.metrics.binders.processor.enabled", havingValue = "false", matchIfMissing = true)
    public ProcessorMetrics processorMetrics() {
        // Return null to disable processor metrics
        return null;
    }

    /**
     * Provide a safe uptime metrics bean that won't cause container issues
     */
    @Bean
    @ConditionalOnProperty(value = "management.metrics.binders.uptime.enabled", havingValue = "true", matchIfMissing = false)
    public UptimeMetrics uptimeMetrics() {
        return new UptimeMetrics();
    }
}
