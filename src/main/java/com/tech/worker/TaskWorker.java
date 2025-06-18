package com.tech.worker;

import com.tech.model.Task;
import com.tech.model.TaskStatus;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskWorker implements Runnable {

    private final Logger logger = Logger.getLogger(TaskWorker.class.getName());

    private final BlockingQueue<Task> taskQueue;
    private final ConcurrentHashMap<UUID, TaskStatus> taskStatuses;
    private final AtomicInteger tasksProcessedCount;
    private volatile boolean running = true;

    public TaskWorker(BlockingQueue<Task> taskQueue, ConcurrentHashMap<UUID, TaskStatus> taskStatuses, AtomicInteger tasksProcessedCount) {
        if (taskQueue == null) {
            throw new IllegalArgumentException("Task queue cannot be null");
        }
        if (taskStatuses == null) {
            throw new IllegalArgumentException("Task statuses map cannot be null");
        }
        if (tasksProcessedCount == null) {
            throw new IllegalArgumentException("Tasks processed count cannot be null");
        }
        this.taskQueue = taskQueue;
        this.taskStatuses = taskStatuses;
        this.tasksProcessedCount = tasksProcessedCount;
        logger.info("TaskWorker initialized and ready to process tasks.");
    }

    @Override
    public void run() {
        String workerName = Thread.currentThread().getName();
        logger.info("Worker " + workerName + " started.");

        while (running) {
            try {
                Task task = (Task) taskQueue.take();
                taskStatuses.put(task.getId(), TaskStatus.PROCESSING);
                logger.log(Level.INFO, String.format("Worker %s processing task: %s. Queue size: %d", workerName, task.getName(), taskQueue.size()));

                long processsDelay = 500L;
                if (task.getPriority() <= 2) {
                    processsDelay = 200L + (long) (Math.random() * 200);
                } else if (task.getPriority() <= 4) {
                    processsDelay = 600L + (long) (Math.random() * 400);
                } else {
                    processsDelay = 400L + (long) (Math.random() * 200);
                }

                Thread.sleep(processsDelay);

                taskStatuses.put(task.getId(), TaskStatus.COMPLETED);
                tasksProcessedCount.incrementAndGet();

                logger.log(Level.INFO, String.format("Worker %s finished processing task: %s. Queue size: %d", workerName, task.getName(), taskQueue.size()));

            } catch (InterruptedException e) {
                logger.log(Level.INFO, String.format("Worker %s interrupted. Shutting down", workerName), e);
            }
        }
        logger.log(Level.INFO, "Worker " + workerName + " terminated.");
    }

    public void stopWorker() {
        this.running = false;
        logger.info("Signaling TaskWorker to stop " + Thread.currentThread().getName() + ". No new tasks will be processed.");
    }
}
