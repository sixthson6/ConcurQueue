package com.tech.monitor;

import com.tech.model.Task;
import com.tech.model.TaskStatus;
import com.tech.model.TaskStatusEntry;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SystemMonitor implements Runnable {

    private static final Logger logger = Logger.getLogger(SystemMonitor.class.getName());
    private static final long MONITOR_INTERVAL_MS = 5000;
    private static final Duration STUCK_TASK_THRESHOLD = Duration.ofSeconds(10);
    private static final long JSON_EXPORT_INTERVAL_SECONDS = 60;
    private static final String JSON_OUTPUT_FILE = "C:\\Users\\ITCompliance\\IdeaProjects\\concurQueue\\task_statuses.json";

    private final BlockingQueue<Task> taskQueue;
    private final ConcurrentHashMap<UUID, TaskStatusEntry> taskStatuses;
    private final ThreadPoolExecutor workerPoolExecutor;
    private final Instant applicationStartTime;
    private volatile boolean running = true;
    private ScheduledExecutorService jsonExportScheduler;

    public SystemMonitor(BlockingQueue<Task> taskQueue,
                         ConcurrentHashMap<UUID, TaskStatusEntry> taskStatuses,
                         ThreadPoolExecutor workerPoolExecutor,
                         Instant applicationStartTime) {
        if (taskQueue == null) {
            throw new IllegalArgumentException("Task queue cannot be null.");
        }
        if (taskStatuses == null) {
            throw new IllegalArgumentException("Task statuses map cannot be null.");
        }
        if (workerPoolExecutor == null) {
            throw new IllegalArgumentException("Worker pool executor cannot be null.");
        }
        if (applicationStartTime == null) {
            throw new IllegalArgumentException("Application start time cannot be null.");
        }
        this.taskQueue = taskQueue;
        this.taskStatuses = taskStatuses;
        this.workerPoolExecutor = workerPoolExecutor;
        this.applicationStartTime = applicationStartTime;
        logger.config("SystemMonitor initialized.");

        jsonExportScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "JsonExportScheduler");
            t.setDaemon(true);
            return t;
        });
        jsonExportScheduler.scheduleAtFixedRate(this::exportTaskStatusesToJsonFile,
                JSON_EXPORT_INTERVAL_SECONDS, JSON_EXPORT_INTERVAL_SECONDS, TimeUnit.SECONDS);
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

        calculateAndLogPerformanceMetrics(completed);
        logger.info("----------------------------");
    }

    private void calculateAndLogPerformanceMetrics(long completedTasksCount) {
        long totalProcessingTimeMillis = taskStatuses.values().stream()
                .filter(s -> s.getStatus() == TaskStatus.COMPLETED && s.getProcessingDuration() != null)
                .mapToLong(s -> s.getProcessingDuration().toMillis())
                .sum();
        long numberOfCompletedTasksWithDuration = taskStatuses.values().stream()
                .filter(s -> s.getStatus() == TaskStatus.COMPLETED && s.getProcessingDuration() != null)
                .count();

        double avgProcessingTimeMillis = 0;
        if (numberOfCompletedTasksWithDuration > 0) {
            avgProcessingTimeMillis = (double) totalProcessingTimeMillis / numberOfCompletedTasksWithDuration;
        }

        Duration applicationUptime = Duration.between(applicationStartTime, Instant.now());
        double throughputTasksPerSecond = 0;
        if (!applicationUptime.isZero() && applicationUptime.toMillis() > 0) {
            throughputTasksPerSecond = (double) completedTasksCount / (applicationUptime.toMillis() / 1000.0);
        }

        logger.info(String.format("Performance Metrics: Avg Proc Time=%.2f ms, Throughput=%.2f tasks/sec",
                avgProcessingTimeMillis, throughputTasksPerSecond));
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

    private void exportTaskStatusesToJsonFile() {
        if (!running) {
            return;
        }

        logger.info("Exporting task statuses to " + JSON_OUTPUT_FILE);
        try (FileWriter writer = new FileWriter(JSON_OUTPUT_FILE)) {
            writer.write("[\n");

            boolean firstEntry = true;
            for (Map.Entry<UUID, TaskStatusEntry> entry : taskStatuses.entrySet()) {
                if (!firstEntry) {
                    writer.write(",\n");
                }
                writer.write("  {\n");
                writer.write(String.format("    \"taskId\": \"%s\",\n", entry.getKey()));
                writer.write(String.format("    \"status\": \"%s\",\n", entry.getValue().getStatus()));
                writer.write(String.format("    \"lastUpdatedTimestamp\": \"%s\",\n", entry.getValue().getLastUpdatedTimestamp()));
                writer.write(String.format("    \"retryAttempts\": %d", entry.getValue().getRetryAttempts()));

                if (entry.getValue().getProcessingStartTime() != null) {
                    writer.write(String.format(",\n    \"processingStartTime\": \"%s\"", entry.getValue().getProcessingStartTime()));
                }
                if (entry.getValue().getProcessingEndTime() != null) {
                    writer.write(String.format(",\n    \"processingEndTime\": \"%s\"", entry.getValue().getProcessingEndTime()));
                }
                if (entry.getValue().getProcessingDuration() != null) {
                    writer.write(String.format(",\n    \"processingDurationMillis\": %d", entry.getValue().getProcessingDuration().toMillis()));
                }

                writer.write("\n  }");
                firstEntry = false;
            }

            writer.write("\n]\n");
            logger.info("Task statuses successfully exported to " + JSON_OUTPUT_FILE);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to export task statuses to JSON file!", e);
        }
    }


    public void stopMonitor() {
        this.running = false;
        if (jsonExportScheduler != null) {
            jsonExportScheduler.shutdown();
            try {
                if (!jsonExportScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warning("JSON export scheduler did not terminate within 5 seconds.");
                    jsonExportScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Monitor interrupted while waiting for JSON scheduler to terminate.", e);
                jsonExportScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        logger.info("Signaling SystemMonitor to stop.");
    }
}
