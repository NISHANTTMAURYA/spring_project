package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for web server properties, including explicit port configuration
 */
@Configuration
public class WebServerConfig {

    @Value("${PORT:8080}")
    private int port;
    
    @Bean
    public WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryCustomizer() {
        return factory -> {
            // Log the port for debugging
            System.out.println("Setting server port to: " + port);
            
            // Explicitly set the port on the factory
            factory.setPort(port);
        };
    }
} 