package com.tech.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

public class Task implements Comparable<Task> {

    private static final Logger logger = Logger.getLogger(Task.class.getName());

    private final UUID id;
    private final String name;
    private final int priority;
    private final Instant createdTimestamp;
    private final String payload;
    private int retryAttempt;

    public static final Task POISON_PILL = new Task("POISON_PILL", Integer.MAX_VALUE, "STOP_SIGNAL", -1);

    private Task(String name, int priority, String payload, int retryAttempt) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Task name cannot be null or empty.");
        }
        if (priority < 1 && !"POISON_PILL".equals(name)) {
            throw new IllegalArgumentException("Task priority must be a positive integer.");
        }
        if (payload == null) {
            throw new IllegalArgumentException("Task payload cannot be null.");
        }
        if (retryAttempt < -1) {
            throw new IllegalArgumentException("Retry attempt cannot be less than -1.");
        }

        this.id = UUID.randomUUID();
        this.name = name;
        this.priority = priority;
        this.createdTimestamp = Instant.now();
        this.payload = payload;
        this.retryAttempt = retryAttempt;

        logger.fine("Task created: " + this);
    }

    public Task(String name, int priority, String payload) {
        this(name, priority, payload, 0);
    }

    public Task(Task originalTask, int newRetryAttempt) {
        this(originalTask.getName(), originalTask.getPriority(), originalTask.getPayload(), newRetryAttempt);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }

    public Instant getCreatedTimestamp() {
        return createdTimestamp;
    }

    public String getPayload() {
        return payload;
    }

    public int getRetryAttempt() {
        return retryAttempt;
    }

    @Override
    public int compareTo(Task other) {
        if (this == POISON_PILL && other != POISON_PILL) {
            return 1;
        }
        if (this != POISON_PILL && other == POISON_PILL) {
            return -1;
        }
        int priorityComparison = Integer.compare(this.priority, other.priority);
        if (priorityComparison != 0) {
            return priorityComparison;
        }
        return this.createdTimestamp.compareTo(other.createdTimestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id.equals(task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        if (this == POISON_PILL) {
            return "Task{type=POISON_PILL}";
        }
        return "Task{" +
                "id=" + id.toString().substring(0, 8) +
                ", name='" + name + '\'' +
                ", priority=" + priority +
                ", created=" + createdTimestamp +
                ", retries=" + retryAttempt +
                '}';
    }
}
