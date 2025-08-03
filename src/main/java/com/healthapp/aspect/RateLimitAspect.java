package com.healthapp.aspect;

import com.healthapp.annotation.RateLimit;
import com.healthapp.service.RateLimitService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

@Aspect
@Component
public class RateLimitAspect {
    
    @Autowired
    private RateLimitService rateLimitService;
    
    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // Get client IP for rate limiting
        String clientKey = getClientKey();
        
        // Check rate limit
        if (!rateLimitService.isAllowed(clientKey, rateLimit.value(), 1)) { // 1 minute window
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                        "error", "Rate limit exceeded",
                        "message", "Too many requests. Please try again later.",
                        "limit", rateLimit.value(),
                        "timeWindow", rateLimit.timeUnit()
                    ));
        }
        
        // Proceed with the method execution
        return joinPoint.proceed();
    }
    
    private String getClientKey() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String clientIp = request.getHeader("X-Forwarded-For");
                if (clientIp == null) {
                    clientIp = request.getHeader("X-Real-IP");
                }
                if (clientIp == null) {
                    clientIp = request.getRemoteAddr();
                }
                return clientIp;
            }
        } catch (Exception e) {
            // Fallback to a default key
        }
        return "unknown";
    }
} 