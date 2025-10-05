package com.privatecal.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.nio.charset.StandardCharsets;

/**
 * Configuration for email templates using Thymeleaf
 */
@Configuration
public class EmailTemplateConfig {

    /**
     * Template resolver for email templates
     * Templates are located in src/main/resources/templates/email/
     */
    @Bean
    public SpringResourceTemplateResolver emailTemplateResolver() {
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setPrefix("classpath:/templates/email/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        templateResolver.setCacheable(false); // Set to true in production
        templateResolver.setOrder(1);
        return templateResolver;
    }

    /**
     * Template engine for processing email templates
     */
    @Bean
    public SpringTemplateEngine emailTemplateEngine() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(emailTemplateResolver());
        templateEngine.setTemplateEngineMessageSource(emailMessageSource());
        templateEngine.setEnableSpringELCompiler(true);
        return templateEngine;
    }

    /**
     * Message source for email translations
     * Properties files are located in src/main/resources/messages/
     */
    @Bean
    public MessageSource emailMessageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages/email");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setFallbackToSystemLocale(false);
        messageSource.setDefaultLocale(java.util.Locale.forLanguageTag("en-US"));
        return messageSource;
    }
}
