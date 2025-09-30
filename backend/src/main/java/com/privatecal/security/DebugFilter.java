package com.privatecal.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class DebugFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String origin = httpRequest.getHeader("Origin");
        String referer = httpRequest.getHeader("Referer");
        String forwardedHost = httpRequest.getHeader("X-Forwarded-Host");
        String forwardedProto = httpRequest.getHeader("X-Forwarded-Proto");

        System.out.println("=== DEBUG REQUEST ===");
        System.out.println("Method: " + httpRequest.getMethod());
        System.out.println("URI: " + httpRequest.getRequestURI());
        System.out.println("Origin: " + origin);
        System.out.println("Referer: " + referer);
        System.out.println("X-Forwarded-Host: " + forwardedHost);
        System.out.println("X-Forwarded-Proto: " + forwardedProto);
        System.out.println("=====================");

        chain.doFilter(request, response);
    }
}
