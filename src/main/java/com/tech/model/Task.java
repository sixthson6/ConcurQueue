package com.tech.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

public class Task implements Comparable<Task>{
    private static final Logger logger = Logger.getLogger(Task.class.getName());

    private final UUID id;
    private final String name;
    private final int priority;
    private final Instant createdTimestamp;
    private final String payload;


    public Task(String name, int priority, String payload) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Task name cannot be null or empty");
        }
        if (priority < 1) {
            throw new IllegalArgumentException("Priority must be a positive integer");
        }
        if (payload == null || payload.trim().isEmpty()) {
            throw new IllegalArgumentException("Payload cannot be null or empty");
        }
        this.id = UUID.randomUUID();
        this.name = name;
        this.priority = priority;
        this.createdTimestamp = Instant.now();
        this.payload = payload;
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

    @Override
    public int compareTo(Task other) {
        int priorityComparison = Integer.compare(this.priority, other.priority);
        if(priorityComparison != 0) {
            return priorityComparison;
        }
        return this.createdTimestamp.compareTo(other.createdTimestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return priority == task.priority && Objects.equals(id, task.id) && Objects.equals(name, task.name) && Objects.equals(createdTimestamp, task.createdTimestamp) && Objects.equals(payload, task.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, priority, createdTimestamp, payload);
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id.toString().substring(0, 8) +
                ", name='" + name + '\'' +
                ", priority=" + priority +
                ", createdTimestamp=" + createdTimestamp +
                '}';
    }

}
