package com.tech.worker;

import com.tech.model.Task;
import com.tech.model.TaskStatus;
import com.tech.model.TaskStatusEntry;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskWorker implements Runnable {

    private static final Logger logger = Logger.getLogger(TaskWorker.class.getName());
    private static final int MAX_RETRIES = 3;

    private final BlockingQueue<Task> taskQueue;
    private final ConcurrentHashMap<UUID, TaskStatusEntry> taskStatuses;
    private final AtomicInteger tasksProcessedCount;
    private volatile boolean running = true;
    private final Random random = new Random();

    public TaskWorker(BlockingQueue<Task> taskQueue,
                      ConcurrentHashMap<UUID, TaskStatusEntry> taskStatuses,
                      AtomicInteger tasksProcessedCount) {
        if (taskQueue == null) {
            throw new IllegalArgumentException("Task queue cannot be null.");
        }
        if (taskStatuses == null) {
            throw new IllegalArgumentException("Task statuses map cannot be null.");
        }
        if (tasksProcessedCount == null) {
            throw new IllegalArgumentException("Tasks processed count cannot be null.");
        }
        this.taskQueue = taskQueue;
        this.taskStatuses = taskStatuses;
        this.tasksProcessedCount = tasksProcessedCount;
        logger.config("TaskWorker initialized.");
    }

    @Override
    public void run() {
        String workerName = Thread.currentThread().getName();
        logger.info(String.format("Worker '%s' started and ready to process tasks.", workerName));

        while (running) {
            Task task = null;
            TaskStatusEntry statusEntry = null;
            try {
                logger.fine(String.format("Worker '%s' waiting for task from queue (queue size: %d)",
                        workerName, taskQueue.size()));

                task = taskQueue.take();

                logger.fine(String.format("Worker '%s' took task from queue: %s", workerName, task.toString()));

                if (task == Task.POISON_PILL) {
                    logger.info(String.format("Worker '%s' received poison pill. Shutting down gracefully.", workerName));
                    taskQueue.put(Task.POISON_PILL);
                    break;
                }

                statusEntry = taskStatuses.get(task.getId());
                if (statusEntry == null) {
                    statusEntry = new TaskStatusEntry(task.getId(), TaskStatus.PROCESSING, task.getRetryAttempt());
                    taskStatuses.put(task.getId(), statusEntry);
                    logger.warning(String.format("Worker '%s' found a task without prior status entry. Task: %s", workerName, task.toString()));
                } else {
                    statusEntry.updateStatus(TaskStatus.PROCESSING);
                }

                statusEntry.setProcessingStartTime(Instant.now());

                logger.info(String.format("Worker '%s' started processing task: %s (Retry: %d). Status: %s. Queue size: %d",
                        workerName, task.toString(), task.getRetryAttempt(), statusEntry.getStatus(), taskQueue.size()));

                long processingDelay = calculateProcessingDelay(task.getPriority());
                Thread.sleep(processingDelay);

                statusEntry.setProcessingEndTime(Instant.now());

                if (shouldTaskFail() && task.getRetryAttempt() < MAX_RETRIES) {
                    handleTaskFailure(task, statusEntry, workerName);
                } else {
                    handleTaskSuccess(task, statusEntry, workerName, processingDelay);
                }

            } catch (InterruptedException e) {
                logger.log(Level.WARNING, String.format("TaskWorker '%s' was interrupted. Attempting graceful shutdown.", workerName), e);
                Thread.currentThread().interrupt();
                running = false;
            } catch (Exception e) {
                logger.log(Level.SEVERE, String.format("An unexpected error occurred in TaskWorker '%s' processing task: %s",
                        workerName, (task != null ? task.toString() : "N/A")), e);
                handleTaskError(task, statusEntry);
            }
        }
        logger.info(String.format("TaskWorker '%s' stopped. Final processed count: %d", workerName, tasksProcessedCount.get()));
    }

    private long calculateProcessingDelay(int priority) {
        if (priority <= 2) {
            return 200L + (long) (Math.random() * 200);
        } else if (priority >= 4) {
            return 600L + (long) (Math.random() * 400);
        } else {
            return 400L + (long) (Math.random() * 200);
        }
    }

    private boolean shouldTaskFail() {
        return random.nextDouble() < 0.35;
    }

    private void handleTaskFailure(Task task, TaskStatusEntry statusEntry, String workerName) {
        try {
            statusEntry.incrementRetryAttempts();
            statusEntry.updateStatus(TaskStatus.FAILED_RETRY);
            Task retryTask = new Task(task, task.getRetryAttempt() + 1);
            taskQueue.put(retryTask);
            taskStatuses.put(retryTask.getId(), new TaskStatusEntry(retryTask.getId(), TaskStatus.SUBMITTED, retryTask.getRetryAttempt()));

            logger.warning(String.format("Worker '%s' failed task: %s. Re-queueing for retry %d (queue size: %d).",
                    workerName, task.toString(), retryTask.getRetryAttempt(), taskQueue.size()));
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Failed to requeue task for retry", e);
            Thread.currentThread().interrupt();
        }
    }

    private void handleTaskSuccess(Task task, TaskStatusEntry statusEntry, String workerName, long processingDelay) {
        if (task.getRetryAttempt() >= MAX_RETRIES && statusEntry.getRetryAttempts() > 0) {
            // Task that failed multiple times but now succeeded on final attempt
            statusEntry.updateStatus(TaskStatus.FAILED_PERMANENTLY);
            logger.severe(String.format("Worker '%s' marked task as permanently failed: %s after %d retries.",
                    workerName, task.toString(), task.getRetryAttempt()));
        } else {
            statusEntry.updateStatus(TaskStatus.COMPLETED);
            int currentCount = tasksProcessedCount.incrementAndGet();
            logger.info(String.format("Worker '%s' completed task: %s (Processed in %d ms). Total processed: %d",
                    workerName, task.toString(), processingDelay, currentCount));
        }
    }

    private void handleTaskError(Task task, TaskStatusEntry statusEntry) {
        if (task != null) {
            if (statusEntry != null) {
                statusEntry.setProcessingEndTime(Instant.now());
                statusEntry.updateStatus(TaskStatus.FAILED_PERMANENTLY);
            } else {
                taskStatuses.put(task.getId(), new TaskStatusEntry(task.getId(), TaskStatus.FAILED_PERMANENTLY, task.getRetryAttempt()));
            }
        }
    }

    public void stopWorker() {
        this.running = false;
        logger.info(String.format("Signaling TaskWorker '%s' to stop.", Thread.currentThread().getName()));
    }
}