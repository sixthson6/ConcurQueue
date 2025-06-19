package com.tech.util;

import java.io.IOException;
import java.util.Scanner;

public class ConsoleUtils {

    private static final Scanner scanner = new Scanner(System.in);
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_PURPLE = "\u001B[35m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";
    private static final String ANSI_BOLD = "\u001B[1m";

    private static final String BOX_TOP_LEFT = "╔";
    private static final String BOX_TOP_RIGHT = "╗";
    private static final String BOX_BOTTOM_LEFT = "╚";
    private static final String BOX_BOTTOM_RIGHT = "╝";
    private static final String BOX_HORIZONTAL = "═";
    private static final String BOX_VERTICAL = "║";

    public static void clearScreen() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[2J\033[H");
                System.out.flush();
            }
        } catch (IOException | InterruptedException e) {
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }

    public static void printHeader(String title) {
        int width = Math.max(title.length() + 4, 60);
        String topBorder = BOX_TOP_LEFT + BOX_HORIZONTAL.repeat(width - 2) + BOX_TOP_RIGHT;
        String bottomBorder = BOX_BOTTOM_LEFT + BOX_HORIZONTAL.repeat(width - 2) + BOX_BOTTOM_RIGHT;
        String titleLine = BOX_VERTICAL + " " + centerText(title, width - 4) + " " + BOX_VERTICAL;

        System.out.println(ANSI_CYAN + ANSI_BOLD + topBorder + ANSI_RESET);
        System.out.println(ANSI_CYAN + ANSI_BOLD + titleLine + ANSI_RESET);
        System.out.println(ANSI_CYAN + ANSI_BOLD + bottomBorder + ANSI_RESET);
        System.out.println();
    }

    public static void printSeparator() {
        System.out.println(ANSI_BLUE + "─".repeat(80) + ANSI_RESET);
    }

    public static void printInfo(String message) {
        System.out.println(ANSI_BLUE + "ℹ️  " + message + ANSI_RESET);
    }

    public static void printSuccess(String message) {
        System.out.println(ANSI_GREEN + "✅ " + message + ANSI_RESET);
    }

    public static void printWarning(String message) {
        System.out.println(ANSI_YELLOW + "⚠️  " + message + ANSI_RESET);
    }

    public static void printError(String message) {
        System.out.println(ANSI_RED + "❌ " + message + ANSI_RESET);
    }

    public static void printHighlight(String message) {
        System.out.println(ANSI_PURPLE + ANSI_BOLD + "🔸 " + message + ANSI_RESET);
    }

    public static void waitForUser(String prompt) {
        System.out.print(ANSI_CYAN + prompt + ANSI_RESET);
        scanner.nextLine();
    }

    public static boolean confirmAction(String message) {
        System.out.print(ANSI_YELLOW + message + " (y/N): " + ANSI_RESET);
        String response = scanner.nextLine().trim().toLowerCase();
        return response.equals("y") || response.equals("yes");
    }

    public static int getIntInput(String prompt, int min, int max) {
        while (true) {
            try {
                System.out.print(ANSI_CYAN + prompt + ANSI_RESET);
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= min && value <= max) {
                    return value;
                } else {
                    printError("Please enter a value between " + min + " and " + max);
                }
            } catch (NumberFormatException e) {
                printError("Please enter a valid number");
            }
        }
    }

    public static String getStringInput(String prompt, boolean allowEmpty) {
        while (true) {
            System.out.print(ANSI_CYAN + prompt + ANSI_RESET);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty() || allowEmpty) {
                return input;
            } else {
                printError("Input cannot be empty");
            }
        }
    }

    public static void showProgress(String task, int current, int total) {
        int progressWidth = 40;
        int progress = (int) ((double) current / total * progressWidth);

        StringBuilder bar = new StringBuilder();
        bar.append("[");
        for (int i = 0; i < progressWidth; i++) {
            if (i < progress) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        bar.append("]");

        double percentage = (double) current / total * 100;
        System.out.print("\r" + ANSI_GREEN + task + ": " + bar.toString() +
                String.format(" %.1f%% (%d/%d)" + ANSI_RESET, percentage, current, total));

        if (current == total) {
            System.out.println();
        }
    }

    public static void printTable(String[] headers, String[][] data) {
        if (headers == null || data == null || headers.length == 0) {
            return;
        }

        int[] columnWidths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            columnWidths[i] = headers[i].length();
        }

        for (String[] row : data) {
            for (int i = 0; i < Math.min(row.length, columnWidths.length); i++) {
                if (row[i] != null) {
                    columnWidths[i] = Math.max(columnWidths[i], row[i].length());
                }
            }
        }

        System.out.print(ANSI_BOLD + ANSI_BLUE);
        for (int i = 0; i < headers.length; i++) {
            System.out.printf("%-" + (columnWidths[i] + 2) + "s", headers[i]);
        }
        System.out.println(ANSI_RESET);

        for (int i = 0; i < headers.length; i++) {
            System.out.print("─".repeat(columnWidths[i] + 2));
        }
        System.out.println();

        for (String[] row : data) {
            for (int i = 0; i < Math.min(row.length, columnWidths.length); i++) {
                String value = row[i] != null ? row[i] : "";
                System.out.printf("%-" + (columnWidths[i] + 2) + "s", value);
            }
            System.out.println();
        }
    }

    public static void printSystemInfo() {
        Runtime runtime = Runtime.getRuntime();

        String[][] systemData = {
                {"JVM Version", System.getProperty("java.version")},
                {"JVM Vendor", System.getProperty("java.vendor")},
                {"OS Name", System.getProperty("os.name")},
                {"OS Version", System.getProperty("os.version")},
                {"Available Processors", String.valueOf(runtime.availableProcessors())},
                {"Max Memory", formatBytes(runtime.maxMemory())},
                {"Total Memory", formatBytes(runtime.totalMemory())},
                {"Free Memory", formatBytes(runtime.freeMemory())},
                {"Used Memory", formatBytes(runtime.totalMemory() - runtime.freeMemory())}
        };

        printTable(new String[]{"Property", "Value"}, systemData);
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private static String centerText(String text, int width) {
        if (text.length() >= width) {
            return text;
        }
        int leftPadding = (width - text.length()) / 2;
        int rightPadding = width - text.length() - leftPadding;
        return " ".repeat(leftPadding) + text + " ".repeat(rightPadding);
    }

    public static String formatDuration(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + "ms";
        } else if (milliseconds < 60000) {
            return String.format("%.2fs", milliseconds / 1000.0);
        } else if (milliseconds < 3600000) {
            long minutes = milliseconds / 60000;
            long seconds = (milliseconds % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        } else {
            long hours = milliseconds / 3600000;
            long minutes = (milliseconds % 3600000) / 60000;
            return String.format("%dh %dm", hours, minutes);
        }
    }

    public static void showLoadingAnimation(String message, int durationMs) {
        String[] animation = {"|", "/", "-", "\\"};
        long startTime = System.currentTimeMillis();
        int frame = 0;

        while (System.currentTimeMillis() - startTime < durationMs) {
            System.out.print("\r" + ANSI_YELLOW + message + " " + animation[frame % animation.length] + ANSI_RESET);
            frame++;
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.print("\r" + " ".repeat(message.length() + 2) + "\r");
    }
}

