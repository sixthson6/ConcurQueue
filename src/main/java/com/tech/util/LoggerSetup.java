package com.tech.util;

import java.io.IOException;
import java.util.logging.*;

public class LoggerSetup {
    public static final String LOG_FILE_PATH = "C:\\Users\\ITCompliance\\IdeaProjects\\concurQueue\\concurqueue.log";
    private static boolean initialized = false;

    public static synchronized void setup() {
        if (initialized) {
            return;
        }
        initialized = true;

        Logger rootLogger = LogManager.getLogManager().getLogger("");
        rootLogger.setLevel(Level.INFO);

        for (java.util.logging.Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new CustomLoggerFormatter());
        consoleHandler.setLevel(Level.INFO);
        rootLogger.addHandler(consoleHandler);

        try {
            FileHandler fileHandler = new FileHandler(LOG_FILE_PATH, true);
            fileHandler.setFormatter(new CustomLoggerFormatter());
            fileHandler.setLevel(Level.ALL);
            rootLogger.addHandler(fileHandler);
            System.out.println("Logging initialized. Logs will be written to " + LOG_FILE_PATH);
        } catch (IOException e) {
            System.err.println("Failed to initialize file logging: " + e.getMessage());
            rootLogger.log(Level.SEVERE, "Failed to initialize file logging: " + e.getMessage());
        }

    }

    public static class CustomLoggerFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            StringBuilder builder = new StringBuilder();
            builder.append("[").append(new java.util.Date(record.getMillis())).append("] ");
            builder.append("[").append(record.getLevel().getName()).append("] ");
            builder.append("[").append(Thread.currentThread().getName()).append("] ");
            builder.append("[").append(record.getSourceClassName()).append(".").append(record.getSourceMethodName()).append("] ");
            builder.append(formatMessage(record));
            builder.append(System.lineSeparator());

            if (record.getThrown() != null) {
                builder.append("Exception: ");
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                builder.append(sw.toString());
                builder.append(System.lineSeparator());
            }
            return builder.toString();
        }
    }
}
