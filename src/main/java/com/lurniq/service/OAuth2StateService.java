package com.lurniq.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OAuth2StateService {
    
    // Thread-safe in-memory storage for OAuth2 states
    private final Map<String, Boolean> oauth2States = new ConcurrentHashMap<>();
    
    public void storeFrontendFlag(String state) {
        oauth2States.put(state, true);
    }
    
    public boolean isFrontendRequest(String state) {
        return oauth2States.getOrDefault(state, false);
    }
    
    public void removeState(String state) {
        oauth2States.remove(state);
    }
    
    // Get the current number of stored states (for monitoring)
    public int getStateCount() {
        return oauth2States.size();
    }
    
    // Cleanup method that can be called periodically to remove old states
    // In production, consider using a cache with TTL like Redis or Caffeine
    public void cleanup() {
        // This could be enhanced to track timestamps and remove old entries
        // For now, it's a placeholder for future enhancement
    }
}
