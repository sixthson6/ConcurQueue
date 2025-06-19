package com.tech;

import com.tech.model.Task;
import com.tech.model.TaskStatus;
import com.tech.producer.TaskProducer;
import com.tech.util.LoggerSetup;
import com.tech.worker.TaskWorker;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ConcurQueueApp {

    private static final Logger logger = Logger.getLogger(ConcurQueueApp.class.getName());
    private static final int NUMBER_OF_PRODUCERS = 3;
    private static final int TASKS_PER_PRODUCER = 10;
    private static final long PRODUCER_INTERVAL_MS = 1500;
    private static final int NUMBER_OF_WORKERS = 4;

    public static void main(String[] args) {
        LoggerSetup.setup();
        logger.info("ConcurQueue Application starting...");
        System.out.println("______________________________________________________________________________");

        BlockingQueue<Task> taskQueue = new PriorityBlockingQueue<>();
        logger.info("Shared Task Queue (PriorityBlockingQueue) initialized.");

        ConcurrentHashMap<UUID, TaskStatus> taskStatuses = new ConcurrentHashMap<>();
        logger.info("Task Status Tracker (ConcurrentHashMap) initialized.");

        AtomicInteger tasksProcessedCount = new AtomicInteger(0);
        logger.info("AtomicInteger for tasks processed count initialized.");
        System.out.println("______________________________________________________________________________");


        logger.info(String.format("Starting %d producer threads...", NUMBER_OF_PRODUCERS));
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
            TaskProducer producer = new TaskProducer(producerName, taskQueue, taskStatuses, TASKS_PER_PRODUCER, PRODUCER_INTERVAL_MS);
            producerThreads[i] = new Thread(producer, producerName);
            producerThreads[i].start();
            logger.info(String.format("Started %s.", producerName));
        }
        System.out.println("______________________________________________________________________________");

        logger.info(String.format("Setting up Worker Pool with %d threads...", NUMBER_OF_WORKERS));
        ExecutorService workerPool = Executors.newFixedThreadPool(NUMBER_OF_WORKERS);
        for (int i = 0; i < NUMBER_OF_WORKERS; i++) {
            workerPool.submit(new TaskWorker(taskQueue, taskStatuses, tasksProcessedCount));
        }
        logger.info("Worker pool threads submitted.");
        System.out.println("______________________________________________________________________________");

        for (int i = 0; i < NUMBER_OF_PRODUCERS; i++) {
            try {
                producerThreads[i].join();
                logger.info("Producer {0} has finished its tasks.");
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Main thread interrupted while waiting for producers.", e);
                Thread.currentThread().interrupt();
            }
        }
        logger.info("All producer threads have completed their task generation.");
        System.out.println("______________________________________________________________________________");

        logger.info("Submitting poison pills to worker queue to signal graceful shutdown.");
        for (int i = 0; i < NUMBER_OF_WORKERS; i++) {
            try {
                taskQueue.put(Task.createPoisonPill());
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Main thread interrupted while submitting poison pill.", e);
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("______________________________________________________________________________");

        logger.info("All tasks and poison pills submitted. Final task status summary:");
        taskStatuses.forEach((uuid, status) ->
                logger.info(String.format("Task ID: %s, Status: %s", uuid.toString().substring(0, 8), status)));
        logger.info(String.format("Total tasks processed count (from AtomicInteger): %d. Total tasks submitted: %d.",
                tasksProcessedCount.get(), (NUMBER_OF_PRODUCERS * TASKS_PER_PRODUCER)));
        System.out.println("______________________________________________________________________________");


        logger.info("Shutting down worker pool...");
        workerPool.shutdown();
        try {
            if (!workerPool.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.warning("Worker pool did not terminate gracefully within the specified timeout. Forcing shutdown.");
                workerPool.shutdownNow();
                if (!workerPool.awaitTermination(30, TimeUnit.SECONDS)) {
                    logger.severe("Worker pool did not terminate after forced shutdown.");
                }
            } else {
                logger.info("Worker Pool terminated successfully.");
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interruption during worker pool shutdown.", e);
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("______________________________________________________________________________");

        logger.info("ConcurQueue Application finished.");
        System.out.println("______________________________________________________________________________");
    }
}
