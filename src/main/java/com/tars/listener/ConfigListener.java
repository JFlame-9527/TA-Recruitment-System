package com.tars.listener;

import com.tars.config.QwenConfiguration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jflame
 * @version 3.0.0
 * @since 2026/4/16
 */
@Slf4j
@WebListener
public class ConfigListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("Initializing Qwen configuration...");
        QwenConfiguration.initialize(sce.getServletContext());
        log.info("Qwen configuration initialized successfully");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("Qwen configuration destroyed");
    }
}