package com.privatecal.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JWT Authentication Filter that validates JWT tokens and sets up Spring Security context
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    private JwtUtils jwtUtils;
    private UserDetailsService userDetailsService;
    
    // Setter methods for dependency injection
    public void setJwtUtils(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }
    
    public void setUserDetailsService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                  @NonNull HttpServletResponse response, 
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Extract JWT token from request
            String jwt = parseJwt(request);
            
            if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Validate the token
                if (jwtUtils.validateAccessToken(jwt)) {
                    // Get username from token
                    String username = jwtUtils.getUsernameFromToken(jwt);
                    
                    if (username != null) {
                        // Load user details
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        
                        // Validate token against user details
                        if (jwtUtils.validateToken(jwt, userDetails)) {
                            // Create authentication token
                            UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            
                            // Set authentication in security context
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            
                            logger.debug("Authentication successful for user: {}", username);
                        } else {
                            logger.warn("JWT token validation failed for user: {}", username);
                        }
                    }
                } else {
                    logger.warn("Invalid or expired JWT token");
                }
            }
        } catch (Exception e) {
            logger.debug("JWT authentication failed: {}", e.getMessage());
            
            // Clear security context on error but don't handle the error here
            // Let Spring Security handle the authentication failure
            SecurityContextHolder.clearContext();
        }
        
        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
    
    /**
     * Extract JWT token from Authorization header
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader(AUTHORIZATION_HEADER);
        
        if (headerAuth != null && headerAuth.startsWith(BEARER_PREFIX)) {
            String token = headerAuth.substring(BEARER_PREFIX.length());
            logger.debug("JWT token extracted from request");
            return token;
        }
        
        return null;
    }
    
    
    /**
     * Check if request should be excluded from JWT authentication
     * Override this method to exclude specific paths
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        // Exclude CalDAV endpoints - they use HTTP Basic Auth instead of JWT
        if (path.startsWith("/caldav/")) {
            return true;
        }

        // Exclude authentication endpoints
        if (path.startsWith("/api/auth/login") ||
            path.startsWith("/api/auth/register") ||
            path.startsWith("/api/auth/refresh")) {
            return true;
        }

        // Exclude public endpoints
        if (path.startsWith("/api/public/") ||
            path.startsWith("/actuator/") ||
            path.equals("/error") ||
            path.equals("/") ||
            path.equals("/favicon.ico") ||
            path.startsWith("/static/") ||
            path.startsWith("/css/") ||
            path.startsWith("/js/") ||
            path.startsWith("/images/")) {
            return true;
        }

        // Exclude Swagger/OpenAPI endpoints in development
        if (path.startsWith("/swagger-ui") ||
            path.startsWith("/v3/api-docs") ||
            path.equals("/swagger-ui.html")) {
            return true;
        }

        return false;
    }
    
    /**
     * Add custom header to response for debugging (in development)
     */
    private void addDebugHeaders(HttpServletResponse response, String jwt) {
        if (logger.isDebugEnabled()) {
            try {
                if (jwt != null) {
                    Long userId = jwtUtils.getUserIdFromToken(jwt);
                    String username = jwtUtils.getUsernameFromToken(jwt);
                    Long minutesUntilExpiration = jwtUtils.getMinutesUntilExpiration(jwt);
                    
                    response.addHeader("X-Debug-User-ID", String.valueOf(userId));
                    response.addHeader("X-Debug-Username", username);
                    response.addHeader("X-Debug-Token-Expiry-Minutes", String.valueOf(minutesUntilExpiration));
                }
            } catch (Exception e) {
                logger.debug("Error adding debug headers", e);
            }
        }
    }
    
    /**
     * Log authentication details for debugging
     */
    private void logAuthenticationDetails(HttpServletRequest request, String username, String jwt) {
        if (logger.isDebugEnabled()) {
            logger.debug("Processing JWT authentication for request: {} {}", 
                        request.getMethod(), request.getRequestURI());
            logger.debug("User: {}, JWT present: {}", username, jwt != null);
            
            if (jwt != null) {
                try {
                    Long minutesUntilExpiration = jwtUtils.getMinutesUntilExpiration(jwt);
                    logger.debug("Token expires in {} minutes", minutesUntilExpiration);
                } catch (Exception e) {
                    logger.debug("Error getting token expiration", e);
                }
            }
        }
    }
}