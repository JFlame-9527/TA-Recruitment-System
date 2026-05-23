package com.tars.listener;

import com.tars.service.AdminService;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled task listener that manages periodic maintenance operations.
 * <p>
 * This listener creates and manages a scheduled executor service that runs background
 * tasks at fixed intervals. Currently, it performs daily position status updates to
 * ensure data consistency and automate position lifecycle management.
 * </p>
 * <p>
 * <b>Scheduled Tasks:</b>
 * <ul>
 *   <li><b>Update Position Status</b>: Runs daily at midnight (00:00) to:
 *     <ul>
 *       <li>Close positions past their deadline</li>
 *       <li>Mark filled positions as complete</li>
 *       <li>Update position statistics</li>
 *     </ul>
 *   </li>
 * </ul>
 * </p>
 * <p>
 * <b>Thread Management:</b>
 * <ul>
 *   <li>Uses a single-threaded scheduled executor with daemon thread</li>
 *   <li>Daemon thread ensures it doesn't prevent JVM shutdown</li>
 *   <li>Graceful shutdown with 10-second timeout during application undeployment</li>
 * </ul>
 * </p>
 * <p>
 * <b>Execution Schedule:</b>
 * <ul>
 *   <li>Initial delay: Calculated dynamically until next midnight</li>
 *   <li>Period: Every 24 hours</li>
 *   <li>Timezone: Server local time</li>
 * </ul>
 * </p>
 * <p>
 * <b>Error Handling:</b> All exceptions during task execution are caught and logged
 * to prevent the scheduler from terminating. This ensures continuous operation even
 * if individual task executions fail.
 * </p>
 *
 * @author Jflame
 * @version 4.0.0
 * @since 2026/5/6
 * @see AdminService#closePositions()
 * @see ServletContextListener
 * @see ScheduledExecutorService
 */
@Slf4j
@WebListener
public class ScheduledTaskListener implements ServletContextListener {

    private ScheduledExecutorService scheduler;

    /**
     * Initializes the scheduled task scheduler when the web application starts.
     * <p>
     * This method performs the following setup:
     * <ol>
     *   <li>Creates a single-threaded scheduled executor with a named daemon thread</li>
     *   <li>Calculates the initial delay until the next midnight (00:00)</li>
     *   <li>Schedules the {@link #updatePositionStatus()} task to run every 24 hours</li>
     *   <li>Logs the initialization status and next execution time</li>
     * </ol>
     * </p>
     * <p>
     * <b>Thread Configuration:</b>
     * <ul>
     *   <li>Thread name: "scheduled-task-thread" (for easy identification in logs)</li>
     *   <li>Daemon mode: true (doesn't block JVM shutdown)</li>
     *   <li>Pool size: 1 (single-threaded for sequential task execution)</li>
     * </ul>
     * </p>
     *
     * @param sce The ServletContextEvent containing the ServletContext
     * @see #calculateInitialDelay(int, int)
     * @see #updatePositionStatus()
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("Initializing scheduled tasks...");

        // Create single-threaded scheduler with daemon thread
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "scheduled-task-thread");
            t.setDaemon(true);
            return t;
        });

        // Schedule daily task at midnight (00:00)
        long initialDelay = calculateInitialDelay(0, 0);
        scheduler.scheduleAtFixedRate(
                this::updatePositionStatus,
                initialDelay,
                24,
                TimeUnit.HOURS
        );

        log.info("Scheduled tasks initialized successfully. Next execution at midnight.");
    }

    /**
     * Gracefully shuts down the scheduled task scheduler when the application stops.
     * <p>
     * This method ensures proper cleanup of background threads during application
     * undeployment or server shutdown:
     * <ol>
     *   <li>Initiates graceful shutdown (allows running tasks to complete)</li>
     *   <li>Waits up to 10 seconds for tasks to finish</li>
     *   <li>If tasks don't complete in time, forces immediate shutdown</li>
     *   <li>Handles interruption gracefully and restores interrupt status</li>
     * </ol>
     * </p>
     * <p>
     * <b>Shutdown Strategy:</b>
     * <ul>
     *   <li>First attempt: {@link ScheduledExecutorService#shutdown()} - graceful</li>
     *   <li>Fallback: {@link ScheduledExecutorService#shutdownNow()} - forced</li>
     *   <li>Timeout: 10 seconds maximum wait time</li>
     * </ul>
     * </p>
     *
     * @param sce The ServletContextEvent containing the ServletContext
     * @see ScheduledExecutorService#shutdown()
     * @see ScheduledExecutorService#shutdownNow()
     * @see ScheduledExecutorService#awaitTermination(long, TimeUnit)
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("Shutting down scheduled tasks...");

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                // Wait up to 10 seconds for tasks to complete
                if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.warn("Scheduled tasks did not terminate in time, forcing shutdown");
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting for scheduler termination", e);
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("Scheduled tasks shut down completed");
    }

    /**
     * Executes the daily position status update task.
     * <p>
     * This task runs every day at midnight and performs the following operations:
     * <ul>
     *   <li>Calls {@link AdminService#closePositions()} to update expired positions</li>
     *   <li>Automatically closes positions past their application deadline</li>
     *   <li>Updates position status based on current date and application counts</li>
     * </ul>
     * </p>
     * <p>
     * <b>Error Handling:</b> All exceptions are caught and logged to prevent the
     * scheduler from terminating. This ensures the task continues to run daily
     * even if individual executions encounter errors.
     * </p>
     * <p>
     * <b>Logging:</b> Uses visual separators (====) to make task execution easily
     * identifiable in log files for monitoring and debugging purposes.
     * </p>
     *
     * @see AdminService#closePositions()
     */
    private void updatePositionStatus() {
        log.info("========================================");
        log.info("Starting scheduled task: Update Position Status");
        log.info("Execution time: {}", LocalDateTime.now());
        log.info("========================================");

        try {
            new AdminService().closePositions();

            log.info("Scheduled task completed successfully: Update Position Status");

        } catch (Exception e) {
            log.error("Error occurred during scheduled task: Update Position Status", e);
        } finally {
            log.info("========================================");
        }
    }

    /**
     * Calculates the initial delay in milliseconds until the next scheduled execution time.
     * <p>
     * This method determines how long to wait before the first execution of a scheduled task.
     * If the specified time has already passed today, it schedules for tomorrow.
     * </p>
     * <p>
     * <b>Example:</b>
     * <pre>
     * Current time: 2026-05-23 14:30:00
     * Target time: 00:00 (midnight)
     * Result: Schedules for 2026-05-24 00:00:00 (9.5 hours delay)
     * 
     * Current time: 2026-05-23 23:50:00
     * Target time: 00:00 (midnight)
     * Result: Schedules for 2026-05-24 00:00:00 (10 minutes delay)
     * </pre>
     * </p>
     *
     * @param hour   Hour of day (0-23) for the scheduled time
     * @param minute Minute of hour (0-59) for the scheduled time
     * @return Delay in milliseconds until the next occurrence of the specified time
     * @see Duration#between(java.time.temporal.Temporal, java.time.temporal.Temporal)
     * @see LocalDateTime#withHour(int)
     */
    private long calculateInitialDelay(int hour, int minute) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.withHour(hour)
                .withMinute(minute)
                .withSecond(0)
                .withNano(0);

        // If the time has already passed today, schedule for tomorrow
        if (!now.isBefore(nextRun)) {
            nextRun = nextRun.plusDays(1);
        }

        long delay = Duration.between(now, nextRun).toMillis();

        log.info("Next execution scheduled at: {}", nextRun);
        log.info("Initial delay: {} hours, {} minutes",
                Duration.ofMillis(delay).toHours(),
                Duration.ofMillis(delay).toMinutesPart());

        return delay;
    }
}
