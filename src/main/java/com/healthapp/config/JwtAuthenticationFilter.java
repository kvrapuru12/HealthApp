package com.healthapp.config;

import com.healthapp.entity.User;
import com.healthapp.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // Remove "Bearer "
            
            try {
                // Parse the simple token format: "user_id_role_timestamp"
                String[] parts = token.split("_");
                if (parts.length >= 3) {
                    Long userId = Long.parseLong(parts[0]);
                    String role = parts[1];
                    
                    // Check if authentication is already set (preserve it during delete operations)
                    // This prevents clearing authentication if account becomes DELETED during request processing
                    Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
                    if (existingAuth != null && existingAuth.getPrincipal() instanceof Long 
                            && existingAuth.getPrincipal().equals(userId)) {
                        // Authentication already set, preserve it
                    } else {
                        // Get user from database
                        User user = userService.getUserById(userId).orElse(null);
                        if (user != null && user.getAccountStatus() == User.AccountStatus.ACTIVE) {
                            
                            // Create authentication token
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userId, // principal - use userId as Long
                                null, // credentials
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                            );
                            
                            // Set authentication in context
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        } else {
                            logger.warn("JWT Filter - User not found or not ACTIVE. User: {}, Status: {}", 
                                    user != null ? user.getId() : "null", user != null ? user.getAccountStatus() : "null");
                        }
                    }
                } else {
                    logger.warn("JWT Filter - Token does not have enough parts. Parts: {}", parts.length);
                }
            } catch (Exception e) {
                // Token parsing failed, continue without authentication
                logger.error("JWT Filter - Failed to parse JWT token: {}", e.getMessage(), e);
            }
        }
        
        filterChain.doFilter(request, response);
    }
} 