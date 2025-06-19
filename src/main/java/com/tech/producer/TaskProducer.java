package com.tech.producer;

import com.tech.model.Task;
import com.tech.model.TaskStatus;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.tech.model.TaskStatusEntry;

public class TaskProducer implements Runnable {

    private static final Logger logger = Logger.getLogger(TaskProducer.class.getName());

    private final String producerName;
    private final BlockingQueue<Task> taskQueue;
    private final ConcurrentHashMap<UUID, TaskStatusEntry> taskStatuses;
    private final int numberOfTasksToGenerate;
    private final long generationIntervalMillis;
    private final Random random = new Random();

    public TaskProducer(String producerName, BlockingQueue<Task> taskQueue,
                        ConcurrentHashMap<UUID, TaskStatusEntry> taskStatuses,
                        int numberOfTasksToGenerate, long generationIntervalMillis) {
        if (producerName == null || producerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Producer name cannot be null or empty.");
        }
        if (taskQueue == null) {
            throw new IllegalArgumentException("Task queue cannot be null.");
        }
        if (taskStatuses == null) {
            throw new IllegalArgumentException("Task statuses map cannot be null.");
        }
        if (numberOfTasksToGenerate <= 0) {
            throw new IllegalArgumentException("Number of tasks to generate must be positive.");
        }
        if (generationIntervalMillis < 0) {
            throw new IllegalArgumentException("Generation interval cannot be negative.");
        }

        this.producerName = producerName;
        this.taskQueue = taskQueue;
        this.taskStatuses = taskStatuses;
        this.numberOfTasksToGenerate = numberOfTasksToGenerate;
        this.generationIntervalMillis = generationIntervalMillis;
        logger.config(String.format("Producer '%s' initialized. Will generate %d tasks every %d ms.",
                producerName, numberOfTasksToGenerate, generationIntervalMillis));
    }

    @Override
    public void run() {
        logger.info(String.format("Producer '%s' started generating tasks.", producerName));
        try {
            for (int i = 0; i < numberOfTasksToGenerate; i++) {
                int priority;
                String taskName;

                if (producerName.contains("HighPriority")) {
                    priority = random.nextInt(3) + 1;
                    taskName = "HP_Task_" + (i + 1);
                } else if (producerName.contains("LowPriority")) {
                    priority = random.nextInt(3) + 3;
                    taskName = "LP_Task_" + (i + 1);
                } else {
                    priority = random.nextInt(5) + 1;
                    taskName = "MIX_Task_" + (i + 1);
                }

                String payload = "Payload for " + taskName + " from " + producerName;
                Task task = new Task(taskName, priority, payload);

                taskQueue.put(task);
                taskStatuses.put(task.getId(), new TaskStatusEntry(task.getId(), TaskStatus.SUBMITTED));
                logger.log(Level.INFO, String.format("Producer '%s' submitted task: %s. Status: %s. Queue size: %d",
                        producerName, task.toString(), TaskStatus.SUBMITTED, taskQueue.size()));

                Thread.sleep(generationIntervalMillis);
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, String.format("Producer '%s' was interrupted while generating tasks.", producerName), e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.log(Level.SEVERE, String.format("An unexpected error occurred in Producer '%s'.", producerName), e);
        }
        logger.info(String.format("Producer '%s' finished generating %d tasks.", producerName, numberOfTasksToGenerate));
    }
}

