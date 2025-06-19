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
        logger.info(String.format("Worker '%s' started.", workerName));

        while (running) {
            Task task = null;
            TaskStatusEntry statusEntry = null;
            try {
                task = taskQueue.take();

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

                logger.log(Level.INFO, String.format("Worker '%s' started processing task: %s (Retry: %d). Status: %s. Queue size: %d",
                        workerName, task.toString(), task.getRetryAttempt(), statusEntry.getStatus(), taskQueue.size()));

                long processingDelay = 500L;
                if (task.getPriority() <= 2) {
                    processingDelay = 200L + (long) (Math.random() * 200);
                } else if (task.getPriority() >= 4) {
                    processingDelay = 600L + (long) (Math.random() * 400);
                } else {
                    processingDelay = 400L + (long) (Math.random() * 200);
                }

                Thread.sleep(processingDelay);

                statusEntry.setProcessingEndTime(Instant.now());

                if (random.nextDouble() < 0.15 && task.getRetryAttempt() < MAX_RETRIES) {
                    statusEntry.incrementRetryAttempts();
                    statusEntry.updateStatus(TaskStatus.FAILED_RETRY);
                    Task retryTask = new Task(task, task.getRetryAttempt() + 1);
                    taskQueue.put(retryTask);
                    logger.warning(String.format("Worker '%s' failed task: %s. Re-queueing for retry %d.",
                            workerName, task.toString(), retryTask.getRetryAttempt()));
                } else {
                    if (task.getRetryAttempt() >= MAX_RETRIES && statusEntry.getStatus() == TaskStatus.FAILED_RETRY) {
                        statusEntry.updateStatus(TaskStatus.FAILED_PERMANENTLY);
                        logger.severe(String.format("Worker '%s' completed/failed task: %s permanently after %d retries.",
                                workerName, task.toString(), task.getRetryAttempt()));
                    } else {
                        statusEntry.updateStatus(TaskStatus.COMPLETED);
                        tasksProcessedCount.incrementAndGet();
                        logger.log(Level.INFO, String.format("Worker '%s' completed task: %s (Processed in %d ms). Total processed: %d",
                                workerName, task.toString(), processingDelay, tasksProcessedCount.get()));
                    }
                }
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, String.format("TaskWorker '%s' was interrupted. Attempting graceful shutdown.", workerName), e);
                Thread.currentThread().interrupt();
                running = false;
            } catch (Exception e) {
                logger.log(Level.SEVERE, String.format("An unexpected error occurred in TaskWorker '%s' processing task: %s",
                        workerName, (task != null ? task.toString() : "N/A")), e);
                if (task != null) {
                    statusEntry = taskStatuses.get(task.getId());
                    if (statusEntry != null) {
                        statusEntry.setProcessingEndTime(Instant.now());
                        statusEntry.updateStatus(TaskStatus.FAILED_PERMANENTLY);
                    } else {
                        taskStatuses.put(task.getId(), new TaskStatusEntry(task.getId(), TaskStatus.FAILED_PERMANENTLY, task.getRetryAttempt()));
                    }
                }
            }
        }
        logger.info(String.format("TaskWorker '%s' stopped.", workerName));
    }

    public void stopWorker() {
        this.running = false;
        logger.info(String.format("Signaling TaskWorker '%s' to stop.", Thread.currentThread().getName()));
    }
}
