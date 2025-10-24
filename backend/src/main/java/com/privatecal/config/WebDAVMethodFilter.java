package com.privatecal.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter to support WebDAV HTTP methods (PROPFIND, PROPPATCH, MKCOL, etc.)
 * that are not standard HTTP methods in Spring.
 *
 * This filter wraps the request to ensure Spring's DispatcherServlet
 * can handle custom HTTP methods used by CalDAV protocol.
 */
@Component
public class WebDAVMethodFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest httpRequest) {
            String method = httpRequest.getMethod();

            // Pass through WebDAV methods (CalDAV uses these)
            if ("PROPFIND".equalsIgnoreCase(method) ||
                "PROPPATCH".equalsIgnoreCase(method) ||
                "MKCOL".equalsIgnoreCase(method) ||
                "REPORT".equalsIgnoreCase(method)) {

                // Wrap the request to preserve the custom method
                HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(httpRequest) {
                    @Override
                    public String getMethod() {
                        return method.toUpperCase();
                    }
                };

                chain.doFilter(wrapper, response);
                return;
            }
        }

        // For all other requests, pass through normally
        chain.doFilter(request, response);
    }
}
