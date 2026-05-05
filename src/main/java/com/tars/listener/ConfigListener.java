package com.tars.listener;

import com.tars.config.ApplicationConfiguration;
import com.tars.config.QwenConfiguration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import lombok.extern.slf4j.Slf4j;

/**
 * Application configuration listener
 * Initializes all configurations on application startup
 *
 * @author Jflame
 * @version 3.0.0
 * @since 2026/4/16
 */
@Slf4j
@WebListener
public class ConfigListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        log.info("=== Application Configuration Initialization ===");
        
        log.info("Initializing Qwen configuration...");
        QwenConfiguration.initialize(servletContext);
        log.info("Qwen configuration initialized successfully");

        log.info("Initializing application configuration...");
        ApplicationConfiguration.initialize(servletContext);
        log.info("Application configuration initialized successfully");

        log.info("=== All Configurations Initialized ===");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("Application configurations destroyed");
    }
}