package com.tech.simulation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RaceConditionDemo {

    private static final Logger logger = Logger.getLogger(RaceConditionDemo.class.getName());

    private static int unsafeCounter = 0;

    private static AtomicInteger safeCounter = new AtomicInteger(0);

    private static final int NUM_THREADS = 5;
    private static final int INCREMENTS_PER_THREAD = 10000;

    public static void runSimulation() {
        logger.info("______________________________________________________________________________");
        logger.info("Starting Race Condition Simulation...");
        logger.info("______________________________________________________________________________");

        unsafeCounter = 0;
        safeCounter.set(0);

        logger.info("--- Demonstrating Race Condition with unsafeCounter (int) ---");
        ExecutorService unsafeExecutor = Executors.newFixedThreadPool(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; i++) {
            unsafeExecutor.submit(() -> {
                for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                    unsafeCounter++;
                }
            });
        }
        unsafeExecutor.shutdown();
        try {
            if (!unsafeExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warning("Unsafe counter threads did not terminate.");
                unsafeExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Unsafe counter executor interrupted during shutdown.", e);
            unsafeExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info(String.format("Expected final count: %d (NUM_THREADS * INCREMENTS_PER_THREAD)", NUM_THREADS * INCREMENTS_PER_THREAD));
        logger.info(String.format("Actual unsafeCounter after race: %d", unsafeCounter));
        if (unsafeCounter != (NUM_THREADS * INCREMENTS_PER_THREAD)) {
            logger.severe("Race condition detected! Unsafe counter result is incorrect.");
        } else {
            logger.info("Unsafe counter result happened to be correct (rare, but possible).");
        }
        logger.info("______________________________________________________________________________");

        logger.info("--- Demonstrating Fix with safeCounter (AtomicInteger) ---");
        ExecutorService safeExecutor = Executors.newFixedThreadPool(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; i++) {
            safeExecutor.submit(() -> {
                for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                    safeCounter.incrementAndGet();
                }
            });
        }
        safeExecutor.shutdown();
        try {
            if (!safeExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warning("Safe counter threads did not terminate.");
                safeExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Safe counter executor interrupted during shutdown.", e);
            safeExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info(String.format("Expected final count: %d", NUM_THREADS * INCREMENTS_PER_THREAD));
        logger.info(String.format("Actual safeCounter after fix: %d", safeCounter.get()));
        if (safeCounter.get() == (NUM_THREADS * INCREMENTS_PER_THREAD)) {
            logger.info("AtomicInteger successfully prevented race condition. Result is correct.");
        } else {
            logger.severe("AtomicInteger did NOT prevent race condition! This should not happen.");
        }
        logger.info("______________________________________________________________________________");
        logger.info("Race Condition Simulation Finished.");
        logger.info("______________________________________________________________________________");
    }
}

