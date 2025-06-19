package com.tech.util;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LoggerSetup {

    public static final String LOG_FILE_PATH = "concurqueue.log";
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
        consoleHandler.setFormatter(new CustomLogFormatter());
        consoleHandler.setLevel(Level.INFO);
        rootLogger.addHandler(consoleHandler);

        try {
            FileHandler fileHandler = new FileHandler(LOG_FILE_PATH, true);
            fileHandler.setFormatter(new CustomLogFormatter());
            fileHandler.setLevel(Level.ALL);
            rootLogger.addHandler(fileHandler);
            System.out.println("Logging initialized. Output to console (with colors) and " + LOG_FILE_PATH);
        } catch (IOException e) {
            System.err.println("Failed to set up file logger: " + e.getMessage());
            rootLogger.log(Level.SEVERE, "Could not set up file logger!", e);
        }
    }

    private static class CustomLogFormatter extends Formatter {
        public static final String ANSI_RESET = "\u001B[0m";
        public static final String ANSI_BLACK = "\u001B[30m";
        public static final String ANSI_RED = "\u001B[31m";
        public static final String ANSI_GREEN = "\u001B[32m";
        public static final String ANSI_YELLOW = "\u001B[33m";
        public static final String ANSI_BLUE = "\u001B[34m";
        public static final String ANSI_PURPLE = "\u001B[35m";
        public static final String ANSI_CYAN = "\u001B[36m";
        public static final String ANSI_WHITE = "\u001B[37m";

        public static final String ANSI_RED_BACKGROUND = "\u001B[41m";
        public static final String ANSI_YELLOW_BACKGROUND = "\u001B[43m";

        @Override
        public String format(LogRecord record) {
            StringBuilder builder = new StringBuilder();
            String color = ANSI_RESET;

            if (record.getLevel() == Level.SEVERE) {
                color = ANSI_RED;
            } else if (record.getLevel() == Level.WARNING) {
                color = ANSI_RED;
            } else if (record.getLevel() == Level.INFO) {
                color = ANSI_GREEN;
            } else if (record.getLevel() == Level.FINE || record.getLevel() == Level.FINER || record.getLevel() == Level.FINER) {
                color = ANSI_WHITE;
            }

            builder.append(color);

            builder.append("[").append(new java.util.Date(record.getMillis())).append("] ");
            builder.append("[").append(record.getLevel().getName()).append("] ");
            builder.append("[").append(Thread.currentThread().getName()).append("] ");
            builder.append("[").append(record.getSourceClassName()).append(".").append(record.getSourceMethodName()).append("] - ");
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

            builder.append(ANSI_RESET);
            return builder.toString();
        }
    }
}
