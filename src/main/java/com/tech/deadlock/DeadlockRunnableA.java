package com.tech.deadlock;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DeadlockRunnableA implements Runnable {

    private static final Logger logger = Logger.getLogger(DeadlockRunnableA.class.getName());

    private final ResourceLock resource1;
    private final ResourceLock resource2;

    public DeadlockRunnableA(ResourceLock resource1, ResourceLock resource2) {
        if (resource1 == null || resource2 == null) {
            throw new IllegalArgumentException("Resources cannot be null.");
        }
        this.resource1 = resource1;
        this.resource2 = resource2;
        logger.config(String.format("DeadlockRunnableA initialized with %s and %s.", resource1.getName(), resource2.getName()));
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        logger.info(String.format("%s: Attempting to lock %s...", threadName, resource1.getName()));
        synchronized (resource1) {
            logger.info(String.format("%s: Locked %s. Now attempting to lock %s...", threadName, resource1.getName(), resource2.getName()));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, String.format("%s: Interrupted while waiting.", threadName), e);
                Thread.currentThread().interrupt();
            }
            synchronized (resource2) {
                logger.info(String.format("%s: Locked %s and %s. Performing work...", threadName, resource1.getName(), resource2.getName()));
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, String.format("%s: Interrupted during work.", threadName), e);
                    Thread.currentThread().interrupt();
                }
                logger.info(String.format("%s: Released %s and %s.", threadName, resource2.getName(), resource1.getName()));
            }
        }
        logger.info(String.format("%s: Finished.", threadName));
    }
}
