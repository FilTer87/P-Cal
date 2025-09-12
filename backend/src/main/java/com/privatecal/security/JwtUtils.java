package com.privatecal.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT utility class for token generation, validation, and parsing
 */
@Component
public class JwtUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    
    @Value("${app.jwt.secret}")
    private String jwtSecret;
    
    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;
    
    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;
    
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String USER_ID_CLAIM = "userId";
    private static final String FULL_NAME_CLAIM = "fullName";
    
    /**
     * Get signing key from secret - create a secure 512-bit key for HS512
     */
    private SecretKey getSigningKey() {
        try {
            // Create a SHA-512 hash of the secret to ensure we have a 512-bit key
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] keyBytes = digest.digest(jwtSecret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-512 algorithm not available, falling back to HS256", e);
            // Fallback: use SHA-256 for HS256 algorithm
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] keyBytes = digest.digest(jwtSecret.getBytes(StandardCharsets.UTF_8));
                return Keys.hmacShaKeyFor(keyBytes);
            } catch (NoSuchAlgorithmException ex) {
                throw new RuntimeException("Neither SHA-512 nor SHA-256 available", ex);
            }
        }
    }
    
    /**
     * Generate access token for user
     */
    public String generateAccessToken(UserDetails userDetails, Long userId, String fullName) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE);
        claims.put(USER_ID_CLAIM, userId);
        claims.put(FULL_NAME_CLAIM, fullName);
        
        return createToken(claims, userDetails.getUsername(), accessTokenExpiration);
    }
    
    /**
     * Generate refresh token for user
     */
    public String generateRefreshToken(UserDetails userDetails, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE);
        claims.put(USER_ID_CLAIM, userId);
        
        return createToken(claims, userDetails.getUsername(), refreshTokenExpiration);
    }
    
    /**
     * Create JWT token with claims and expiration
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }
    
    /**
     * Get username from JWT token
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }
    
    /**
     * Get user ID from JWT token
     */
    public Long getUserIdFromToken(String token) {
        final Claims claims = getAllClaimsFromToken(token);
        Object userIdClaim = claims.get(USER_ID_CLAIM);
        if (userIdClaim instanceof Integer) {
            return ((Integer) userIdClaim).longValue();
        } else if (userIdClaim instanceof Long) {
            return (Long) userIdClaim;
        }
        return null;
    }
    
    /**
     * Get full name from JWT token
     */
    public String getFullNameFromToken(String token) {
        final Claims claims = getAllClaimsFromToken(token);
        return (String) claims.get(FULL_NAME_CLAIM);
    }
    
    /**
     * Get token type from JWT token
     */
    public String getTokenTypeFromToken(String token) {
        final Claims claims = getAllClaimsFromToken(token);
        return (String) claims.get(TOKEN_TYPE_CLAIM);
    }
    
    /**
     * Get expiration date from JWT token
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }
    
    /**
     * Get expiration date as LocalDateTime
     */
    public LocalDateTime getExpirationLocalDateTimeFromToken(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    
    /**
     * Get issued at date from JWT token
     */
    public Date getIssuedAtFromToken(String token) {
        return getClaimFromToken(token, Claims::getIssuedAt);
    }
    
    /**
     * Get specific claim from JWT token
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
    
    /**
     * Get all claims from JWT token
     */
    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            logger.error("Error parsing JWT token: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Check if JWT token is expired
     */
    public Boolean isTokenExpired(String token) {
        try {
            final Date expiration = getExpirationDateFromToken(token);
            return expiration.before(new Date());
        } catch (JwtException e) {
            logger.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }
    
    /**
     * Check if token is access token
     */
    public Boolean isAccessToken(String token) {
        try {
            String tokenType = getTokenTypeFromToken(token);
            return ACCESS_TOKEN_TYPE.equals(tokenType);
        } catch (JwtException e) {
            logger.error("Error checking token type: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if token is refresh token
     */
    public Boolean isRefreshToken(String token) {
        try {
            String tokenType = getTokenTypeFromToken(token);
            return REFRESH_TOKEN_TYPE.equals(tokenType);
        } catch (JwtException e) {
            logger.error("Error checking token type: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Validate JWT token against user details
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (JwtException e) {
            logger.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Validate JWT token (basic validation)
     */
    public Boolean validateToken(String token) {
        try {
            getAllClaimsFromToken(token);
            return !isTokenExpired(token);
        } catch (JwtException e) {
            logger.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Validate access token specifically
     */
    public Boolean validateAccessToken(String token) {
        return validateToken(token) && isAccessToken(token);
    }
    
    /**
     * Validate refresh token specifically
     */
    public Boolean validateRefreshToken(String token) {
        return validateToken(token) && isRefreshToken(token);
    }
    
    /**
     * Get time until token expiration in minutes
     */
    public Long getMinutesUntilExpiration(String token) {
        try {
            Date expiration = getExpirationDateFromToken(token);
            Date now = new Date();
            if (expiration.before(now)) {
                return 0L;
            }
            return (expiration.getTime() - now.getTime()) / (1000 * 60);
        } catch (JwtException e) {
            logger.error("Error calculating minutes until expiration: {}", e.getMessage());
            return 0L;
        }
    }
    
    /**
     * Get access token expiration time as LocalDateTime
     */
    public LocalDateTime getAccessTokenExpirationTime() {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpiration);
        return expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    
    /**
     * Get refresh token expiration time as LocalDateTime
     */
    public LocalDateTime getRefreshTokenExpirationTime() {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenExpiration);
        return expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
    
    /**
     * Extract token from Authorization header
     */
    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
    
    /**
     * Check if token is close to expiry (within 5 minutes)
     */
    public Boolean isTokenCloseToExpiry(String token) {
        try {
            Long minutesUntilExpiration = getMinutesUntilExpiration(token);
            return minutesUntilExpiration != null && minutesUntilExpiration <= 5;
        } catch (Exception e) {
            logger.error("Error checking if token is close to expiry: {}", e.getMessage());
            return true;
        }
    }
    
    /**
     * Parse token and return summary information
     */
    public Map<String, Object> getTokenInfo(String token) {
        Map<String, Object> info = new HashMap<>();
        try {
            Claims claims = getAllClaimsFromToken(token);
            info.put("username", claims.getSubject());
            info.put("userId", claims.get(USER_ID_CLAIM));
            info.put("fullName", claims.get(FULL_NAME_CLAIM));
            info.put("tokenType", claims.get(TOKEN_TYPE_CLAIM));
            info.put("issuedAt", claims.getIssuedAt());
            info.put("expiresAt", claims.getExpiration());
            info.put("isExpired", isTokenExpired(token));
            info.put("minutesUntilExpiration", getMinutesUntilExpiration(token));
        } catch (JwtException e) {
            logger.error("Error getting token info: {}", e.getMessage());
            info.put("error", "Invalid token");
        }
        return info;
    }
}