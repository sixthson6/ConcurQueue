package com.tech.monitor;

import com.tech.model.Task;
import com.tech.model.TaskStatus;
import com.tech.model.TaskStatusEntry;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SystemMonitor implements Runnable {

    private static final Logger logger = Logger.getLogger(SystemMonitor.class.getName());
    private static final long MONITOR_INTERVAL_MS = 5000;
    private static final Duration STUCK_TASK_THRESHOLD = Duration.ofSeconds(10);

    private final BlockingQueue<Task> taskQueue;
    private final ConcurrentHashMap<UUID, TaskStatusEntry> taskStatuses;
    private final ThreadPoolExecutor workerPoolExecutor;
    private volatile boolean running = true;

    public SystemMonitor(BlockingQueue<Task> taskQueue,
                         ConcurrentHashMap<UUID, TaskStatusEntry> taskStatuses,
                         ThreadPoolExecutor workerPoolExecutor) {
        if (taskQueue == null) {
            throw new IllegalArgumentException("Task queue cannot be null.");
        }
        if (taskStatuses == null) {
            throw new IllegalArgumentException("Task statuses map cannot be null.");
        }
        if (workerPoolExecutor == null) {
            throw new IllegalArgumentException("Worker pool executor cannot be null.");
        }
        this.taskQueue = taskQueue;
        this.taskStatuses = taskStatuses;
        this.workerPoolExecutor = workerPoolExecutor;
        logger.config("SystemMonitor initialized.");
    }

    @Override
    public void run() {
        logger.info("SystemMonitor started.");
        while (running) {
            try {
                Thread.sleep(MONITOR_INTERVAL_MS);
                logSystemStatus();
                detectStuckTasks();
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "SystemMonitor interrupted. Shutting down.", e);
                Thread.currentThread().interrupt();
                running = false;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "An unexpected error occurred in SystemMonitor.", e);
            }
        }
        logger.info("SystemMonitor stopped.");
    }

    private void logSystemStatus() {
        logger.info("--- SYSTEM STATUS REPORT ---");
        logger.info(String.format("Task Queue Size: %d", taskQueue.size()));
        logger.info(String.format("Worker Pool - Active Threads: %d, Queued Tasks: %d, Completed Tasks: %d, Pool Size: %d",
                workerPoolExecutor.getActiveCount(),
                workerPoolExecutor.getQueue().size(),
                workerPoolExecutor.getCompletedTaskCount(),
                workerPoolExecutor.getPoolSize()));

        long submitted = taskStatuses.values().stream().filter(s -> s.getStatus() == TaskStatus.SUBMITTED).count();
        long processing = taskStatuses.values().stream().filter(s -> s.getStatus() == TaskStatus.PROCESSING).count();
        long completed = taskStatuses.values().stream().filter(s -> s.getStatus() == TaskStatus.COMPLETED).count();
        long failedRetry = taskStatuses.values().stream().filter(s -> s.getStatus() == TaskStatus.FAILED_RETRY).count();
        long failedPerm = taskStatuses.values().stream().filter(s -> s.getStatus() == TaskStatus.FAILED_PERMANENTLY).count();
        long totalTracked = taskStatuses.size();

        logger.info(String.format("Task Status Summary: Submitted=%d, Processing=%d, Completed=%d, Failed (Retry)=%d, Failed (Permanent)=%d, Total Tracked=%d",
                submitted, processing, completed, failedRetry, failedPerm, totalTracked));
        logger.info("----------------------------");
    }

    private void detectStuckTasks() {
        Instant now = Instant.now();
        for (Map.Entry<UUID, TaskStatusEntry> entry : taskStatuses.entrySet()) {
            TaskStatusEntry statusEntry = entry.getValue();
            if (statusEntry.getStatus() == TaskStatus.PROCESSING) {
                Duration timeInProcessing = Duration.between(statusEntry.getLastUpdatedTimestamp(), now);
                if (timeInProcessing.compareTo(STUCK_TASK_THRESHOLD) > 0) {
                    logger.warning(String.format("STUCK TASK ALERT: Task ID %s has been in PROCESSING state for %s. Last updated: %s",
                            statusEntry.getTaskId().toString().substring(0, 8), timeInProcessing, statusEntry.getLastUpdatedTimestamp()));
                }
            }
        }
    }

    public void stopMonitor() {
        this.running = false;
        logger.info("Signaling SystemMonitor to stop.");
    }
}
