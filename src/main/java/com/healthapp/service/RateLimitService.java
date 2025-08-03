package com.healthapp.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
    
    private final Map<String, RequestTracker> requestCounts = new ConcurrentHashMap<>();
    
    public boolean isAllowed(String key, int maxRequests, int timeWindowMinutes) {
        LocalDateTime now = LocalDateTime.now();
        RequestTracker tracker = requestCounts.computeIfAbsent(key, k -> new RequestTracker());
        
        // Clean old requests outside the time window
        tracker.cleanOldRequests(now, timeWindowMinutes);
        
        // Check if we're under the limit
        if (tracker.getRequestCount() < maxRequests) {
            tracker.addRequest(now);
            return true;
        }
        
        return false;
    }
    
    private static class RequestTracker {
        private final java.util.Queue<LocalDateTime> requests = new java.util.LinkedList<>();
        
        public void addRequest(LocalDateTime time) {
            requests.offer(time);
        }
        
        public void cleanOldRequests(LocalDateTime now, int timeWindowMinutes) {
            LocalDateTime cutoff = now.minusMinutes(timeWindowMinutes);
            while (!requests.isEmpty() && requests.peek().isBefore(cutoff)) {
                requests.poll();
            }
        }
        
        public int getRequestCount() {
            return requests.size();
        }
    }
} 