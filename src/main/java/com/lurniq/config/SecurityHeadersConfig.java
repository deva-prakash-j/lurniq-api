package com.lurniq.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SecurityHeadersConfig implements WebMvcConfigurer {

    @Bean
    public StaticHeadersWriter contentSecurityPolicyHeaderWriter() {
        return new StaticHeadersWriter("Content-Security-Policy", 
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self'; " +
            "connect-src 'self'; " +
            "frame-ancestors 'none'");
    }

    @Bean
    public StaticHeadersWriter xContentTypeOptionsHeaderWriter() {
        return new StaticHeadersWriter("X-Content-Type-Options", "nosniff");
    }

    @Bean
    public ReferrerPolicyHeaderWriter referrerPolicyHeaderWriter() {
        return new ReferrerPolicyHeaderWriter(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN);
    }
}
