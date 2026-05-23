package com.tars.listener;

import com.tars.config.ApplicationConfiguration;
import com.tars.config.QwenConfiguration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import lombok.extern.slf4j.Slf4j;

/**
 * Application configuration listener that initializes all system configurations on startup.
 * <p>
 * This listener is automatically invoked by the servlet container when the web application
 * starts up. It ensures that all required configurations are loaded and initialized before
 * any requests are processed.
 * </p>
 * <p>
 * <b>Initialization Order:</b>
 * <ol>
 *   <li><b>QwenConfiguration</b>: Loads AI service configuration (API key, base URL, model parameters)</li>
 *   <li><b>ApplicationConfiguration</b>: Loads application settings (environment, data paths, logging level)</li>
 * </ol>
 * </p>
 * <p>
 * <b>Configuration Details:</b>
 * <ul>
 *   <li>QwenConfiguration: Reads from {@code qwen_config.json} for AI model settings</li>
 *   <li>ApplicationConfiguration: Reads from {@code config.json} for app environment and data management</li>
 * </ul>
 * </p>
 * <p>
 * <b>Lifecycle:</b>
 * <ul>
 *   <li>{@link #contextInitialized(ServletContextEvent)}: Called once at application startup</li>
 *   <li>{@link #contextDestroyed(ServletContextEvent)}: Called once at application shutdown</li>
 * </ul>
 * </p>
 * <p>
 * <b>Annotation:</b> Marked with {@link WebListener} for automatic registration with the servlet container.
 * No web.xml configuration required.
 * </p>
 *
 * @author Jflame
 * @version 3.0.0
 * @since 2026/4/16
 * @see ApplicationConfiguration
 * @see QwenConfiguration
 * @see ServletContextListener
 */
@Slf4j
@WebListener
public class ConfigListener implements ServletContextListener {

    /**
     * Initializes all application configurations when the web application starts.
     * <p>
     * This method is called exactly once by the servlet container during application
     * deployment. It performs the following initialization steps in order:
     * </p>
     * <ol>
     *   <li>Retrieves the {@link ServletContext} from the event</li>
     *   <li>Initializes {@link QwenConfiguration} with AI service settings</li>
     *   <li>Initializes {@link ApplicationConfiguration} with application environment settings</li>
     *   <li>Logs completion status for verification</li>
     * </ol>
     * <p>
     * If any initialization step fails, a {@link RuntimeException} is thrown, which
     * prevents the application from starting. This ensures that the system never runs
     * with incomplete or invalid configuration.
     * </p>
     *
     * @param sce The ServletContextEvent containing the ServletContext
     * @throws RuntimeException if configuration initialization fails
     * @see QwenConfiguration#initialize(ServletContext)
     * @see ApplicationConfiguration#initialize(ServletContext)
     */
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

    /**
     * Cleanup handler called when the web application is shutting down.
     * <p>
     * This method is invoked by the servlet container during application undeployment
     * or server shutdown. Currently, it serves as a lifecycle marker for logging purposes.
     * </p>
     * <p>
     * <b>Note:</b> Configuration objects are singleton instances that don't require
     * explicit cleanup. Resources like thread pools and file handles are managed by
     * their respective classes.
     * </p>
     *
     * @param sce The ServletContextEvent containing the ServletContext
     * @see ServletContextListener#contextDestroyed(ServletContextEvent)
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("Application configurations destroyed");
    }
}