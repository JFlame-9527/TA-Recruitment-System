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
 * @author Jflame
 * @version 4.0.0
 * @since 2026/5/6
 */
@Slf4j
@WebListener
public class ScheduledTaskListener implements ServletContextListener {
    private ScheduledExecutorService scheduler;

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
     * Update position status task
     * Executes daily at midnight to check and update position statuses
     * (e.g., close expired positions, mark filled positions, etc.)
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
     * Calculate initial delay until the next execution time
     *
     * @param hour Hour of day (0-23)
     * @param minute Minute of hour (0-59)
     * @return Delay in milliseconds until next execution
     */
    private long calculateInitialDelay(int hour, int minute) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextRun = now.withHour(hour)
                .withMinute(minute)
                .withSecond(0)
                .withNano(0);

        // If the time has already passed today, schedule for tomorrow
        if (now.compareTo(nextRun) >= 0) {
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
