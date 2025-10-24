package com.privatecal.security;

import com.privatecal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for JWT-based authentication
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final org.springframework.security.web.firewall.HttpFirewall httpFirewall;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;
    
    @Value("${app.cors.allowed-methods}")
    private String allowedMethods;
    
    @Value("${app.cors.allowed-headers}")
    private String allowedHeaders;
    
    @Value("${app.cors.allow-credentials}")
    private boolean allowCredentials;
    
    /**
     * Password encoder bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // Strength of 12 for good security/performance balance
    }
    
    /**
     * UserDetailsService implementation
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            // Try to find user by username first
            return userRepository.findByUsername(username)
                    .or(() -> userRepository.findByEmail(username)) // Fallback to email
                    .map(UserDetailsImpl::build)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        };
    }
    
    /**
     * Authentication provider
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    /**
     * Authentication manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    /**
     * JWT Authentication Filter
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter();
        filter.setJwtUtils(jwtUtils);
        filter.setUserDetailsService(userDetailsService());
        return filter;
    }
    
    /**
     * CORS configuration
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Use allowedOriginPatterns to support proxy scenarios
        // This allows more flexible origin matching
        configuration.setAllowedOriginPatterns(List.of(allowedOrigins.split(",")));
        
        // Set allowed methods (split by comma)
        configuration.setAllowedMethods(List.of(allowedMethods.split(",")));
        
        // Set allowed headers
        if (allowedHeaders.contains("*")) {
            configuration.addAllowedHeader("*");
        } else {
            configuration.setAllowedHeaders(List.of(allowedHeaders.split(",")));
        }
        
        // Set credentials
        configuration.setAllowCredentials(allowCredentials);
        
        // Expose common headers
        configuration.setExposedHeaders(List.of(
            "Authorization",
            "X-Total-Count",
            "X-Page-Number",
            "X-Page-Size",
            "X-Debug-User-ID",
            "X-Debug-Username",
            "X-Debug-Token-Expiry-Minutes"
        ));
        
        // Set max age for preflight requests
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    /**
     * Security filter chain configuration
     *
     * Supports two authentication mechanisms:
     * - HTTP Basic Auth for CalDAV endpoints (/caldav/**)
     * - JWT Bearer token for REST API endpoints (/api/**)
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // CSRF protection disabled for stateless REST API with JWT authentication
            // CSRF attacks exploit automatic cookie submission, but JWT tokens are sent
            // explicitly in Authorization headers and cannot be exploited this way.
            // Reference: https://security.stackexchange.com/questions/166724/should-i-use-csrf-protection-on-rest-api-endpoints
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh", "/api/auth/forgot-password", "/api/auth/reset-password", "/api/auth/verify-email", "/api/auth/resend-verification").permitAll()
                .requestMatchers("/api/public/**").permitAll()

                // Error handling endpoint
                .requestMatchers("/error").permitAll()

                // Health check and actuator endpoints
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // Static resources
                .requestMatchers("/", "/favicon.ico", "/static/**", "/css/**", "/js/**", "/images/**").permitAll()

                // Swagger/OpenAPI endpoints (allow in development)
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                // OPTIONS requests for CORS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // CalDAV endpoints - require authentication via Basic Auth
                .requestMatchers("/caldav/**").authenticated()

                // All API endpoints require authentication
                .requestMatchers("/api/**").authenticated()

                // All other requests require authentication
                .anyRequest().authenticated()
            )
            // Enable HTTP Basic authentication for CalDAV
            .httpBasic(basic -> basic
                .realmName("PrivateCal CalDAV Server")
            )
            .authenticationProvider(authenticationProvider())
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jwtAuthenticationEntryPoint())
            )
            .addFilterBefore(jwtAuthenticationFilter(), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
    /**
     * Exception handling for authentication failures
     */
    @Bean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }
    
    /**
     * Custom authentication entry point for JWT
     */
    public static class JwtAuthenticationEntryPoint implements org.springframework.security.web.AuthenticationEntryPoint {
        
        @Override
        public void commence(jakarta.servlet.http.HttpServletRequest request,
                           jakarta.servlet.http.HttpServletResponse response,
                           org.springframework.security.core.AuthenticationException authException)
                throws java.io.IOException {
            
            response.setContentType("application/json");
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding("UTF-8");
            
            String jsonResponse = String.format(
                "{\"error\": \"Unauthorized\", \"message\": \"%s\", \"path\": \"%s\", \"timestamp\": \"%s\"}", 
                authException.getMessage(),
                request.getRequestURI(),
                java.time.Instant.now().toString()
            );
            
            response.getWriter().write(jsonResponse);
        }
    }
}