package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
    
    @Component
    public static class PortLogger {
        private final Environment environment;
        private static final Logger logger = Logger.getLogger(PortLogger.class.getName());
        
        public PortLogger(Environment environment) {
            this.environment = environment;
        }
        
        @EventListener
        public void onApplicationStarted(ApplicationStartedEvent event) {
            String port = environment.getProperty("server.port", "8080");
            String configuredPort = environment.getProperty("PORT", "Not set");
            logger.info("Application started with server.port=" + port + " and env PORT=" + configuredPort);
        }
    }
}