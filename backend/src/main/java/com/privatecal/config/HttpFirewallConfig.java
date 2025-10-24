package com.privatecal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

import java.util.Arrays;

/**
 * Configuration to allow WebDAV HTTP methods (PROPFIND, PROPPATCH, etc.)
 * that are not standard HTTP methods but are required for CalDAV protocol (RFC 4791)
 */
@Configuration
public class HttpFirewallConfig {

    @Bean
    public HttpFirewall allowWebDAVMethodsFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();

        // Allow standard HTTP methods plus WebDAV/CalDAV methods
        firewall.setAllowedHttpMethods(Arrays.asList(
            "GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS", "PATCH",
            "PROPFIND", "PROPPATCH", "MKCOL", "COPY", "MOVE", "LOCK", "UNLOCK", "REPORT"
        ));

        return firewall;
    }
}
