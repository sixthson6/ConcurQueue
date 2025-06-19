package com.tech.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

public class TaskStatusEntry {

    private static final Logger logger = Logger.getLogger(TaskStatusEntry.class.getName());

    private final UUID taskId;
    private TaskStatus status;
    private Instant lastUpdatedTimestamp;
    private int retryAttempts;

    public TaskStatusEntry(UUID taskId, TaskStatus status) {
        this(taskId, status, 0);
    }

    public TaskStatusEntry(UUID taskId, TaskStatus status, int retryAttempts) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID cannot be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("Task status cannot be null");
        }
        if (retryAttempts < 0) {
            throw new IllegalArgumentException("Retry attempts cannot be negative");
        }

        this.taskId = taskId;
        this.status = status;
        this.lastUpdatedTimestamp = Instant.now();
        this.retryAttempts = retryAttempts;

        logger.fine("Created TaskStatusEntry: " + this);
    }

    public UUID getTaskId() {
        return taskId;
    }

    public synchronized TaskStatus getStatus() {
        return status;
    }

    public synchronized Instant getLastUpdatedTimestamp() {
        return lastUpdatedTimestamp;
    }

    public synchronized int getRetryAttempts() {
        return retryAttempts;
    }

    public synchronized void updateStatus(TaskStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null.");
        }
        this.status = newStatus;
        this.lastUpdatedTimestamp = Instant.now();
        logger.fine(String.format("Task ID: %s status updated to %s",
                taskId.toString().substring(0, 8), newStatus));
    }

    public synchronized void incrementRetryAttempts() {
        this.retryAttempts++;
        logger.fine(String.format("Task ID: %s retry attempts incremented to %d",
                taskId.toString().substring(0, 8), this.retryAttempts));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TaskStatusEntry that = (TaskStatusEntry) o;
        return retryAttempts == that.retryAttempts && Objects.equals(taskId, that.taskId) && status == that.status && Objects.equals(lastUpdatedTimestamp, that.lastUpdatedTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId, status, lastUpdatedTimestamp, retryAttempts);
    }

    @Override
    public String toString() {
        return "TaskStatusEntry{" +
                "taskId=" + taskId +
                ", status=" + status +
                ", lastUpdatedTimestamp=" + lastUpdatedTimestamp +
                ", retryAttempts=" + retryAttempts +
                '}';
    }
}
