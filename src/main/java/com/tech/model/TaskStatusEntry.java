package com.tech.model;


import java.time.Duration;
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
    private Instant processingStartTime;
    private Instant processingEndTime;

    public TaskStatusEntry(UUID taskId, TaskStatus status) {
        this(taskId, status, 0);
    }

    public TaskStatusEntry(UUID taskId, TaskStatus status, int retryAttempts) {
        if (taskId == null) {
            throw new IllegalArgumentException("Task ID cannot be null.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Task status cannot be null.");
        }
        if (retryAttempts < 0) {
            throw new IllegalArgumentException("Retry attempts cannot be negative.");
        }
        this.taskId = taskId;
        this.status = status;
        this.lastUpdatedTimestamp = Instant.now();
        this.retryAttempts = retryAttempts;
        this.processingStartTime = null;
        this.processingEndTime = null;
        logger.fine(String.format("TaskStatusEntry created for Task ID: %s, Status: %s",
                taskId.toString().substring(0, 8), status));
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

    public synchronized Instant getProcessingStartTime() {
        return processingStartTime;
    }

    public synchronized Instant getProcessingEndTime() {
        return processingEndTime;
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

    public synchronized void setProcessingStartTime(Instant processingStartTime) {
        this.processingStartTime = processingStartTime;
        logger.fine(String.format("Task ID: %s processing started at %s",
                taskId.toString().substring(0, 8), processingStartTime));
    }

    public synchronized void setProcessingEndTime(Instant processingEndTime) {
        this.processingEndTime = processingEndTime;
        logger.fine(String.format("Task ID: %s processing ended at %s",
                taskId.toString().substring(0, 8), processingEndTime));
    }

    public synchronized Duration getProcessingDuration() {
        if (processingStartTime != null && processingEndTime != null) {
            return Duration.between(processingStartTime, processingEndTime);
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskStatusEntry that = (TaskStatusEntry) o;
        return taskId.equals(that.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }

    @Override
    public String toString() {
        return "TaskStatusEntry{" +
                "taskId=" + taskId.toString().substring(0, 8) +
                ", status=" + status +
                ", lastUpdated=" + lastUpdatedTimestamp +
                ", retries=" + retryAttempts +
                (processingStartTime != null ? ", startTime=" + processingStartTime : "") +
                (processingEndTime != null ? ", endTime=" + processingEndTime : "") +
                (getProcessingDuration() != null ? ", duration=" + getProcessingDuration().toMillis() + "ms" : "") +
                '}';
    }
}


