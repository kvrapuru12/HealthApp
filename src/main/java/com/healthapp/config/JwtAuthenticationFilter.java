package com.healthapp.config;

import com.healthapp.entity.User;
import com.healthapp.service.JwtAccessTokenService;
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

    @Autowired
    private JwtAccessTokenService jwtAccessTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
                token = token.substring(7).trim();
            }
            
            try {
                if (JwtAccessTokenService.looksLikeJwt(token)) {
                    jwtAccessTokenService.parseValidAccessToken(token).ifPresent(claims -> {
                        Long userId = claims.userId();
                        String role = claims.role();
                        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
                        if (existingAuth != null && existingAuth.getPrincipal() instanceof Long
                                && existingAuth.getPrincipal().equals(userId)) {
                            return;
                        }
                        User user = userService.getUserById(userId).orElse(null);
                        if (user != null && user.getAccountStatus() == User.AccountStatus.ACTIVE) {
                            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userId,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                            );
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                        } else {
                            logger.warn("JWT Filter - User not found or not ACTIVE. User: {}, Status: {}",
                                    user != null ? user.getId() : "null", user != null ? user.getAccountStatus() : "null");
                        }
                    });
                } else {
                    // Legacy access token: userId_role_timestamp (optional leading "Bearer " already stripped from header value)
                    String legacy = token;
                    if (legacy.regionMatches(true, 0, "Bearer ", 0, 7)) {
                        legacy = legacy.substring(7).trim();
                    }
                    String[] parts = legacy.split("_");
                    if (parts.length >= 3) {
                        Long userId = Long.parseLong(parts[0]);
                        String role = parts[1];
                        
                        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
                        if (existingAuth != null && existingAuth.getPrincipal() instanceof Long 
                                && existingAuth.getPrincipal().equals(userId)) {
                            // Authentication already set, preserve it
                        } else {
                            User user = userService.getUserById(userId).orElse(null);
                            if (user != null && user.getAccountStatus() == User.AccountStatus.ACTIVE) {
                                
                                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                    userId,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                                );
                                
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            } else {
                                logger.warn("JWT Filter - User not found or not ACTIVE. User: {}, Status: {}", 
                                        user != null ? user.getId() : "null", user != null ? user.getAccountStatus() : "null");
                            }
                        }
                    } else {
                        logger.warn("JWT Filter - Legacy token does not have enough parts. Parts: {}", parts.length);
                    }
                }
            } catch (Exception e) {
                // Token parsing failed, continue without authentication
                logger.error("JWT Filter - Failed to parse JWT token: {}", e.getMessage(), e);
            }
        }
        
        filterChain.doFilter(request, response);
    }
} 