package com.tech;

import com.tech.deadlock.DeadlockRunnableA;
import com.tech.deadlock.DeadlockRunnableB;
import com.tech.deadlock.ResourceLock;
import com.tech.model.Task;
import com.tech.model.TaskStatusEntry;
import com.tech.monitor.SystemMonitor;
import com.tech.producer.TaskProducer;
import com.tech.util.LoggerSetup;
import com.tech.worker.TaskWorker;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConcurQueueApp {

    private static final Logger logger = Logger.getLogger(ConcurQueueApp.class.getName());
    private static final int NUMBER_OF_PRODUCERS = 3;
    private static final int TASKS_PER_PRODUCER = 10;
    private static final long PRODUCER_INTERVAL_MS = 1500;
    private static final int NUMBER_OF_WORKERS = 4;
    private static final int QUEUE_CAPACITY = 10;

    private static final ResourceLock LOCK_A = new ResourceLock("LockA");
    private static final ResourceLock LOCK_B = new ResourceLock("LockB");


    public static void main(String[] args) {
        LoggerSetup.setup();
        Instant applicationStartTime = Instant.now();
        logger.info("ConcurQueue Application starting...");
        System.out.println("______________________________________________________________________________");

        BlockingQueue<Task> taskQueue = new PriorityBlockingQueue<>(QUEUE_CAPACITY);
        logger.info(String.format("Shared Task Queue (PriorityBlockingQueue) initialized with capacity %d.", QUEUE_CAPACITY));

        ConcurrentHashMap<UUID, TaskStatusEntry> taskStatuses = new ConcurrentHashMap<>();
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
        ThreadPoolExecutor workerPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(NUMBER_OF_WORKERS);
        for (int i = 0; i < NUMBER_OF_WORKERS; i++) {
            workerPool.submit(new TaskWorker(taskQueue, taskStatuses, tasksProcessedCount));
        }
        logger.info("Worker pool threads submitted.");
        System.out.println("______________________________________________________________________________");

        SystemMonitor monitor = new SystemMonitor(taskQueue, taskStatuses, workerPool, applicationStartTime);
        Thread monitorThread = new Thread(monitor, "SystemMonitorThread");
        monitorThread.setDaemon(true);
        monitorThread.start();
        logger.info("System Monitor started.");
        System.out.println("______________________________________________________________________________");

        logger.info("Starting Deadlock Demonstration Threads...");
        Thread deadlockThread1 = new Thread(new DeadlockRunnableA(LOCK_A, LOCK_B), "Deadlock-Thread-A");
        Thread deadlockThread2 = new Thread(new DeadlockRunnableB(LOCK_A, LOCK_B), "Deadlock-Thread-B");

        deadlockThread1.start();
        deadlockThread2.start();
        logger.info("Deadlock demonstration threads started. Observe console for deadlock logs.");
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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook activated. Initiating graceful shutdown sequence...");
            monitor.stopMonitor();
            try {
                monitorThread.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (deadlockThread1.isAlive()) {
                logger.info("Shutdown hook: Interrupting Deadlock-Thread-A.");
                deadlockThread1.interrupt();
            }
            if (deadlockThread2.isAlive()) {
                logger.info("Shutdown hook: Interrupting Deadlock-Thread-B.");
                deadlockThread2.interrupt();
            }
            try {
                deadlockThread1.join(2000);
                deadlockThread2.join(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }


            logger.info("Shutdown hook: Submitting poison pills to worker queue.");
            for (int i = 0; i < NUMBER_OF_WORKERS; i++) {
                try {
                    taskQueue.put(Task.POISON_PILL);
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "Shutdown hook interrupted while submitting poison pill.", e);
                    Thread.currentThread().interrupt();
                }
            }

            try {
                logger.info("Shutdown hook: Waiting for tasks to complete and workers to shut down...");
                workerPool.shutdown();
                if (!workerPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    logger.warning("Shutdown hook: Worker Pool did not terminate gracefully. Forcing shutdown.");
                    workerPool.shutdownNow();
                    if (!workerPool.awaitTermination(30, TimeUnit.SECONDS)) {
                        logger.severe("Shutdown hook: Worker Pool did not terminate after forced shutdown.");
                    }
                } else {
                    logger.info("Shutdown hook: Worker Pool terminated successfully.");
                }
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Shutdown hook interrupted while waiting for worker pool termination.", e);
                workerPool.shutdownNow();
                Thread.currentThread().interrupt();
            }

            logger.info("Shutdown hook: All resources released. Application terminated.");
            System.out.println("______________________________________________________________________________");
        }, "ShutdownHookThread"));


        logger.info("Main thread waiting for JVM shutdown signal. Press Ctrl+C to terminate and observe deadlock if it occurs.");
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            logger.log(Level.INFO, "Main thread interrupted. Exiting.");
            Thread.currentThread().interrupt();
        }

        logger.info("ConcurQueue App has finished execution (main thread path).");
        System.out.println("______________________________________________________________________________");
    }
}
