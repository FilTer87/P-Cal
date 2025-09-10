package com.privatecal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

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
            logger.error("Cannot set user authentication in security context", e);
            
            // Clear security context on error
            SecurityContextHolder.clearContext();
            
            // Set error response for JWT errors
            if (isJwtError(e)) {
                handleJwtError(response, e);
                return;
            }
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
     * Check if the exception is a JWT-related error
     */
    private boolean isJwtError(Exception e) {
        return e instanceof io.jsonwebtoken.JwtException || 
               e.getCause() instanceof io.jsonwebtoken.JwtException;
    }
    
    /**
     * Handle JWT-specific errors
     */
    private void handleJwtError(HttpServletResponse response, Exception e) throws IOException {
        logger.error("JWT error: {}", e.getMessage());
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String errorMessage = determineErrorMessage(e);
        String jsonResponse = String.format(
            "{\"error\": \"Unauthorized\", \"message\": \"%s\", \"timestamp\": \"%s\"}", 
            errorMessage, 
            java.time.Instant.now().toString()
        );
        
        response.getWriter().write(jsonResponse);
    }
    
    /**
     * Determine appropriate error message based on exception type
     */
    private String determineErrorMessage(Exception e) {
        if (e instanceof io.jsonwebtoken.ExpiredJwtException) {
            return "JWT token has expired";
        } else if (e instanceof io.jsonwebtoken.UnsupportedJwtException) {
            return "JWT token is unsupported";
        } else if (e instanceof io.jsonwebtoken.MalformedJwtException) {
            return "JWT token is malformed";
        } else if (e instanceof io.jsonwebtoken.security.SignatureException) {
            return "JWT signature validation failed";
        } else if (e instanceof io.jsonwebtoken.security.SecurityException) {
            return "JWT security validation failed";
        } else if (e instanceof IllegalArgumentException) {
            return "JWT token is invalid";
        } else {
            return "JWT token authentication failed";
        }
    }
    
    /**
     * Check if request should be excluded from JWT authentication
     * Override this method to exclude specific paths
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Exclude authentication endpoints
        if (path.startsWith("/api/auth/login") || 
            path.startsWith("/api/auth/register") ||
            path.startsWith("/api/auth/refresh")) {
            return true;
        }
        
        // Exclude public endpoints
        if (path.startsWith("/api/public/") ||
            path.startsWith("/actuator/") ||
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