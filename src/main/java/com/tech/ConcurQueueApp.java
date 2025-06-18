package com.tech;

import com.tech.model.Task;
import com.tech.model.TaskStatus;
import com.tech.producer.TaskProducer;
import com.tech.util.LoggerSetup;
import com.tech.worker.TaskWorker;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConcurQueueApp {

    private static final Logger logger = Logger.getLogger(ConcurQueueApp.class.getName());
    private static final int NUMBER_OF_PRODUCERS = 3;
    private static final int TASKS_PER_PRODUCER = 10;
    private static final long PRODUCER_INTERVAL_MILLIS = 1500;
    private static final int NUMBER_OF_WORKERS = 4;

    public static void main(String[] args) {
        LoggerSetup.setup();
        logger.log(Level.INFO, "Starting ConcurQueue App...");

        BlockingQueue<Task> taskQueue = new PriorityBlockingQueue<>();
        logger.log(Level.INFO, "Shared Task Queue (PriorityBlockingQueue) initialized.");

        ConcurrentHashMap<UUID, TaskStatus> taskStatuses = new ConcurrentHashMap<>();
        logger.log(Level.INFO, "Task Status Map (ConcurrentHashMap) initialized.");

        AtomicInteger tasksProcessedCount = new AtomicInteger(0);
        logger.log(Level.INFO, "Tasks Processed Count (AtomicInteger) initialized.");

        System.out.println("------------------------------------------------------------------------------");

        logger.log(Level.INFO, "Starting {0} producer threads...", NUMBER_OF_PRODUCERS);
        Thread[] producerThreads = new Thread[NUMBER_OF_PRODUCERS];
        for (int i = 0; i < NUMBER_OF_PRODUCERS; i++) {
            String producerName;
            if (i == 0) {
                producerName = "Producer-HighPriority-" + (i + 1);
            } else if (i == 1) {
                producerName = "Producer-LowPriority-" + (i + 1);
            } else {
                producerName = "Producer-MixedPriority-" + (i + 1);
            }

            TaskProducer producer = new TaskProducer(
                    producerName, taskQueue, taskStatuses, TASKS_PER_PRODUCER, PRODUCER_INTERVAL_MILLIS);
            producerThreads[i] = new Thread(producer, producerName);
            producerThreads[i].start();
            logger.log(Level.INFO, "Producer thread {0} started.", producerName);

            System.out.println("------------------------------------------------------------------------------");
        }


        logger.log(Level.INFO, "Setting up Worker Pool with {0} worker threads...", NUMBER_OF_WORKERS);
        ExecutorService workerPool = Executors.newFixedThreadPool(NUMBER_OF_WORKERS);
        for (int i = 0; i < NUMBER_OF_WORKERS; i++) {
            workerPool.submit(new TaskWorker(taskQueue, taskStatuses, tasksProcessedCount));

        }
        logger.info("Worker Pool threads submitted");

        for (int i = 0; i < NUMBER_OF_PRODUCERS; i++) {
            try {
                producerThreads[i].join();
                logger.info(String.format("Producer {0} has finished its tasks", producerThreads[i].getName()));
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Main thread interrupted while waiting for producer {0} to finish", producerThreads[i].getName());
                Thread.currentThread().interrupt();
            }
            System.out.println("------------------------------------------------------------------------------");
        }
        logger.info("All producers have finished their tasks generation.");



        logger.info("Waiting for workers to drain the queue. Current queue size: {0}" + taskQueue.size());
        try {
            long lastQueueSize = -1;
            int stableCount = 0;
            while (!taskQueue.isEmpty() || stableCount < 3) {
                int currentQueueSize = taskQueue.size();
                logger.fine("Queue not empty. waiting ... Current queue size: " + taskQueue.size());
                if (currentQueueSize == lastQueueSize) {
                    stableCount++;
                } else {
                    stableCount = 0;
                }
                lastQueueSize = currentQueueSize;
                Thread.sleep(2000);
            }
            logger.info("All tasks have been processed. Queue is empty now. Giving workers a chance to finish.");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Main thread interrupted while waiting for worker to finish");
            Thread.currentThread().interrupt();
        }

        logger.info("All tasks seem to be processed or queue drained. Final task status summary:");
        taskStatuses.forEach((uuid, status) ->
                logger.info(String.format("Task ID: %s, Status: %s", uuid.toString().substring(0, 8), status)));
        logger.info(String.format("Total tasks processed count (from AtomicInteger): %d. Total tasks submitted: %d.",
                tasksProcessedCount.get(), (NUMBER_OF_PRODUCERS * TASKS_PER_PRODUCER)));

        System.out.println("------------------------------------------------------------------------------");

        logger.info("Shutting down Worker Pool...");
        workerPool.shutdown();
        try {
            if (!workerPool.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                logger.warning("Worker Pool did not terminate in the specified time. Forcing shutdown.");
                workerPool.shutdownNow();
                if (!workerPool.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS)) {
                    logger.severe("Worker Pool did not terminate after forced shutdown.");
                } else {
                    logger.info("Worker Pool terminated successfully after forced shutdown.");
                }
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Main thread interrupted while waiting for worker pool to terminate", e);
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("ConcurQueue App has finished execution.");
    }
}