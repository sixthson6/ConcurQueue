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
        logger.info(String.format("Producer '%s' started generating %d tasks", producerName, numberOfTasksToGenerate));

        boolean isHighPriority = producerName.contains("HighPriority");
        boolean isLowPriority = producerName.contains("LowPriority");
        String taskPrefix = isHighPriority ? "HP_Task_" : isLowPriority ? "LP_Task_" : "MIX_Task_";

        try {
            for (int i = 0; i < numberOfTasksToGenerate; i++) {
                int priority;
                if (isHighPriority) {
                    priority = random.nextInt(3) + 1;
                } else if (isLowPriority) {
                    priority = random.nextInt(3) + 3;
                } else {
                    priority = random.nextInt(5) + 1;
                }

                String taskName = taskPrefix + (i + 1);
                String payload = "Payload for " + taskName + " from " + producerName;
                Task task = new Task(taskName, priority, payload);

                taskQueue.put(task);
                taskStatuses.put(task.getId(), new TaskStatusEntry(task.getId(), TaskStatus.SUBMITTED));

                if (i % 10 == 0 || i == numberOfTasksToGenerate - 1) {
                    logger.info(String.format("Producer '%s': %d/%d tasks submitted. Queue: %d",
                            producerName, i + 1, numberOfTasksToGenerate, taskQueue.size()));
                }

                if (generationIntervalMillis > 0) {
                    Thread.sleep(generationIntervalMillis);
                }
            }
        } catch (InterruptedException e) {
            logger.warning(String.format("Producer '%s' interrupted", producerName));
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.log(Level.SEVERE, String.format("Producer '%s' error", producerName), e);
        }

        logger.info(String.format("Producer '%s' completed %d tasks", producerName, numberOfTasksToGenerate));
    }
}

