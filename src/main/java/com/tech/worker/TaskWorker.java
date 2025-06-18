package com.tech.worker;

import com.tech.model.Task;

import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskWorker implements Runnable {

    private final Logger logger = Logger.getLogger(TaskWorker.class.getName());

    private final BlockingQueue<Task> taskQueue;
    private volatile boolean running = true;

    public TaskWorker(BlockingQueue<Task> taskQueue) {
        if (taskQueue == null) {
            throw new IllegalArgumentException("Task queue cannot be null");
        }
        this.taskQueue = taskQueue;
        logger.info("TaskWorker initialized and ready to process tasks.");
    }

    @Override
    public void run() {
        String workerName = Thread.currentThread().getName();
        logger.info("Worker " + workerName + " started.");

        while (running) {
            try {
                Task task = (Task) taskQueue.take();
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
