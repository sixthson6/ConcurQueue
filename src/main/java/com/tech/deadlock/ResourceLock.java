package com.tech.deadlock;

import java.util.logging.Logger;

public class ResourceLock {

    private static final Logger logger = Logger.getLogger(ResourceLock.class.getName());

    private final String name;

    public ResourceLock(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource name cannot be null or empty.");
        }
        this.name = name;
        logger.fine(String.format("ResourceLock '%s' created.", name));
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "ResourceLock{" +
                "name='" + name + '\'' +
                '}';
    }
}
