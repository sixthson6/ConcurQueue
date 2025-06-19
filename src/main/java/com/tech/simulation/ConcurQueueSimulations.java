package com.tech.simulation;

import com.tech.deadlock.DeadlockRunnableA;
import com.tech.deadlock.DeadlockRunnableB;
import com.tech.deadlock.ResourceLock;
import com.tech.model.Task;
import com.tech.model.TaskStatusEntry;
import com.tech.monitor.SystemMonitor;
import com.tech.producer.TaskProducer;
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



public class ConcurQueueSimulations {

    private static final Logger logger = Logger.getLogger(ConcurQueueSimulations.class.getName());
    private static final int NUMBER_OF_PRODUCERS = 3;
    private static final int TASKS_PER_PRODUCER = 10;
    private static final long PRODUCER_INTERVAL_MS = 1500;
    private static final int NUMBER_OF_WORKERS = 4;
    private static final int QUEUE_CAPACITY = 10;

    private static final ResourceLock LOCK_A = new ResourceLock("LockA");
    private static final ResourceLock LOCK_B = new ResourceLock("LockB");

    public static void runFullSimulation() {
        logger.info("______________________________________________________________________________");
        logger.info("Starting Full ConcurQueue Simulation (Phase 5)...");
        logger.info("______________________________________________________________________________");

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

        logger.info("Main thread: Submitting poison pills to worker queue to signal graceful shutdown.");
        for (int i = 0; i < NUMBER_OF_WORKERS; i++) {
            try {
                taskQueue.put(Task.POISON_PILL);
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Main thread interrupted while submitting poison pill.", e);
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("______________________________________________________________________________");


        logger.info("Main thread: Waiting for all tasks to be processed and for worker pool to terminate.");
        monitor.stopMonitor();
        try {
            monitorThread.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        workerPool.shutdown();
        try {
            if (!workerPool.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.warning("Main thread: Worker Pool did not terminate gracefully within the specified timeout. Forcing shutdown.");
                workerPool.shutdownNow();
                if (!workerPool.awaitTermination(30, TimeUnit.SECONDS)) {
                    logger.severe("Main thread: Worker Pool did not terminate after forced shutdown.");
                }
            } else {
                logger.info("Main thread: Worker Pool terminated successfully.");
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Main thread interrupted while waiting for worker pool termination.", e);
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("______________________________________________________________________________");
        logger.info("Full ConcurQueue Simulation Finished.");
        logger.info("______________________________________________________________________________");
    }

    public static void runDeadlockSimulation() {
        logger.info("______________________________________________________________________________");
        logger.info("Starting Deadlock Simulation...");
        logger.info("______________________________________________________________________________");

        ResourceLock localLockA = new ResourceLock("LocalLockA");
        ResourceLock localLockB = new ResourceLock("LocalLockB");

        Thread deadlockThread1 = new Thread(new DeadlockRunnableA(localLockA, localLockB), "Deadlock-Thread-A");
        Thread deadlockThread2 = new Thread(new DeadlockRunnableB(localLockA, localLockB), "Deadlock-Thread-B");

        deadlockThread1.start();
        deadlockThread2.start();

        logger.info("Deadlock demonstration threads started. Observe console for deadlock logs.");
        logger.info("Press Ctrl+C to terminate the application and observe the threads being interrupted.");
        logger.info("These threads are designed to deadlock, so they will not complete on their own.");
        System.out.println("______________________________________________________________________________");

        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            logger.log(Level.INFO, "Main thread interrupted during deadlock simulation join. Exiting.", e);
            Thread.currentThread().interrupt();
        }

        logger.info("Deadlock Simulation interrupted/finished (main thread path).");
        logger.info("______________________________________________________________________________");
    }
}
