package com.tech;


import com.tech.simulation.ConcurQueueSimulations;
import com.tech.simulation.RaceConditionDemo;
import com.tech.util.ConsoleUtils;
import com.tech.util.LoggerSetup;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConcurQueueApp {

    private static final Logger logger = Logger.getLogger(ConcurQueueApp.class.getName());
    private static volatile boolean applicationRunning = true;
    private static Scanner scanner;

    public static void main(String[] args) {
        LoggerSetup.setup();
        scanner = new Scanner(System.in);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Application shutdown initiated...");
            applicationRunning = false;
            performGlobalCleanup();
            logger.info("Application shutdown completed.");
        }, "GlobalShutdownHook"));

        displayWelcomeMessage();
        runMainApplicationLoop();

        scanner.close();
        logger.info("Application terminated gracefully.");
    }

    private static void displayWelcomeMessage() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printHeader("CONCURQUEUE - MULTITHREADED JOB PROCESSING PLATFORM");
        System.out.println("Welcome to ConcurQueue - A comprehensive demonstration of:");
        System.out.println("• Concurrent programming patterns");
        System.out.println("• Thread synchronization mechanisms");
        System.out.println("• Performance monitoring and optimization");
        System.out.println("• JVM profiling and memory management");
        ConsoleUtils.printSeparator();
        ConsoleUtils.waitForUser("Press Enter to continue...");
    }

    private static void runMainApplicationLoop() {
        int choice = -1;

        while (choice != 0 && applicationRunning) {
            try {
                displayMainMenu();
                choice = getValidChoice();

                if (choice == 0) {
                    handleGracefulExit();
                    break;
                }

                executeChoice(choice);

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Unexpected error in main application loop", e);
                ConsoleUtils.printError("An unexpected error occurred: " + e.getMessage());
                ConsoleUtils.waitForUser("Press Enter to continue...");
            }
        }
    }

    private static void displayMainMenu() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printHeader("CONCURQUEUE SIMULATION MENU");

        System.out.println("Available Simulations:");
        System.out.println();
        System.out.println("1. 🔒 Deadlock Simulation");
        System.out.println("   └─ Demonstrates circular dependency deadlocks (requires Ctrl+C to terminate)");
        System.out.println();
        System.out.println("2. ⚡ Race Condition Demo");
        System.out.println("   └─ Shows thread safety issues and atomic operations");
        System.out.println();
        System.out.println("3. 🚀 Full ConcurQueue Simulation");
        System.out.println("   └─ Complete demo with producers, consumers, monitoring & retry logic");
        System.out.println();
        System.out.println("4. 📊 Performance Analysis");
        System.out.println("   └─ JVM profiling, memory analysis, and performance metrics");
        System.out.println();
        System.out.println("5. ⚙️  Configuration & Settings");
        System.out.println("   └─ Adjust simulation parameters and logging levels");
        System.out.println();
        System.out.println("0. 🚪 Exit Application");

        ConsoleUtils.printSeparator();
        System.out.print("Enter your choice (0-5): ");
    }

    private static int getValidChoice() {
        while (true) {
            try {
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    System.out.print("Please enter a choice: ");
                    continue;
                }

                int choice = Integer.parseInt(input);
                if (choice >= 0 && choice <= 5) {
                    return choice;
                } else {
                    System.out.print("Invalid choice. Please enter a number between 0 and 5: ");
                }
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Please enter a number.");
                logger.log(Level.WARNING, "Invalid number format input", e);
            }
        }
    }

    private static void executeChoice(int choice) {
        try {
            switch (choice) {
                case 1:
                    handleDeadlockSimulation();
                    break;
                case 2:
                    handleRaceConditionDemo();
                    break;
                case 3:
                    handleFullSimulation();
                    break;
                case 4:
                    handlePerformanceAnalysis();
                    break;
                case 5:
                    handleConfigurationMenu();
                    break;
                default:
                    ConsoleUtils.printError("Invalid choice selected");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error executing choice: " + choice, e);
            ConsoleUtils.printError("Failed to execute simulation: " + e.getMessage());
            ConsoleUtils.waitForUser("Press Enter to continue...");
        }
    }

    private static void handleDeadlockSimulation() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printHeader("DEADLOCK SIMULATION");

        System.out.println("⚠️  WARNING: This simulation will create a deadlock scenario!");
        System.out.println();
        System.out.println("What happens:");
        System.out.println("• Two threads will attempt to acquire locks in opposite order");
        System.out.println("• This creates a circular dependency causing deadlock");
        System.out.println("• The application will hang and require Ctrl+C to terminate");
        System.out.println();
        System.out.println("Educational Value:");
        System.out.println("• Demonstrates classic deadlock conditions");
        System.out.println("• Shows importance of lock ordering");
        System.out.println("• Illustrates thread dump analysis");

        if (ConsoleUtils.confirmAction("Do you want to proceed with the deadlock simulation?")) {
            ConsoleUtils.printInfo("Starting deadlock simulation... Use Ctrl+C to terminate.");
            ConsoleUtils.waitForUser("Press Enter to begin...");
            ConcurQueueSimulations.runDeadlockSimulation();
        }
    }

    private static void handleRaceConditionDemo() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printHeader("RACE CONDITION DEMONSTRATION");

        System.out.println("This simulation demonstrates:");
        System.out.println("• Unsafe shared variable access");
        System.out.println("• Thread-safe alternatives using atomic operations");
        System.out.println("• Performance comparison between approaches");
        System.out.println();

        ConsoleUtils.waitForUser("Press Enter to start the race condition demo...");

        long startTime = System.currentTimeMillis();
        RaceConditionDemo.runSimulation();
        long endTime = System.currentTimeMillis();

        ConsoleUtils.printSuccess("Race condition demo completed in " + (endTime - startTime) + "ms");
        ConsoleUtils.waitForUser("Press Enter to return to main menu...");
    }

    private static void handleFullSimulation() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printHeader("FULL CONCURQUEUE SIMULATION");

        System.out.println("This comprehensive simulation includes:");
        System.out.println("• Multiple producer threads generating tasks");
        System.out.println("• Priority-based task queue management");
        System.out.println("• Worker thread pool for task processing");
        System.out.println("• Real-time system monitoring");
        System.out.println("• Retry logic for failed tasks");
        System.out.println("• JSON export of task statuses");
        System.out.println("• Performance metrics collection");
        System.out.println();

        if (ConsoleUtils.confirmAction("Start the full simulation?")) {
            long startTime = System.currentTimeMillis();
            ConsoleUtils.printInfo("Initializing full simulation...");

            try {
                ConcurQueueSimulations.runFullSimulation();
                long endTime = System.currentTimeMillis();
                ConsoleUtils.printSuccess("Full simulation completed successfully in " +
                        ((endTime - startTime) / 1000.0) + " seconds");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Full simulation failed", e);
                ConsoleUtils.printError("Simulation failed: " + e.getMessage());
            }

            ConsoleUtils.waitForUser("Press Enter to return to main menu...");
        }
    }

    private static void handlePerformanceAnalysis() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printHeader("PERFORMANCE ANALYSIS");

        System.out.println("Available Analysis Options:");
        System.out.println();
        System.out.println("1. Memory Usage Analysis");
        System.out.println("2. Thread Pool Performance");
        System.out.println("3. GC Analysis");
        System.out.println("4. System Resource Monitor");
        System.out.println("0. Back to Main Menu");
        System.out.println();

        int choice = getSubMenuChoice(4);
        if (choice > 0) {
            performAnalysis(choice);
        }
    }

    private static void handleConfigurationMenu() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printHeader("CONFIGURATION & SETTINGS");

        System.out.println("Configuration Options:");
        System.out.println();
        System.out.println("1. Adjust Logging Level");
        System.out.println("2. Modify Thread Pool Size");
        System.out.println("3. Change Task Generation Rate");
        System.out.println("4. Set Queue Capacity");
        System.out.println("5. Configure Retry Parameters");
        System.out.println("0. Back to Main Menu");
        System.out.println();

        int choice = getSubMenuChoice(5);
        if (choice > 0) {
            configureSettings(choice);
        }
    }

    private static int getSubMenuChoice(int maxChoice) {
        while (true) {
            try {
                System.out.print("Enter your choice (0-" + maxChoice + "): ");
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    System.out.print("Please enter a choice: ");
                    continue;
                }

                int choice = Integer.parseInt(input);
                if (choice >= 0 && choice <= maxChoice) {
                    return choice;
                } else {
                    System.out.println("Invalid choice. Please try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }

    private static void performAnalysis(int analysisType) {
        switch (analysisType) {
            case 1:
                analyzeMemoryUsage();
                break;
            case 2:
                analyzeThreadPoolPerformance();
                break;
            case 3:
                analyzeGarbageCollection();
                break;
            case 4:
                monitorSystemResources();
                break;
        }
    }

    private static void analyzeMemoryUsage() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printHeader("MEMORY USAGE ANALYSIS");

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        System.out.println("Current Memory Statistics:");
        System.out.printf("Max Memory:    %,d bytes (%.2f MB)%n", maxMemory, maxMemory / 1024.0 / 1024.0);
        System.out.printf("Total Memory:  %,d bytes (%.2f MB)%n", totalMemory, totalMemory / 1024.0 / 1024.0);
        System.out.printf("Used Memory:   %,d bytes (%.2f MB)%n", usedMemory, usedMemory / 1024.0 / 1024.0);
        System.out.printf("Free Memory:   %,d bytes (%.2f MB)%n", freeMemory, freeMemory / 1024.0 / 1024.0);
        System.out.printf("Memory Usage: %.2f%%%n", (usedMemory * 100.0) / totalMemory);

        System.out.println("\nRecommendations:");
        if (usedMemory > totalMemory * 0.8) {
            System.out.println("⚠️  High memory usage detected. Consider increasing heap size.");
        } else {
            System.out.println("✅ Memory usage is within acceptable limits.");
        }

        ConsoleUtils.waitForUser("Press Enter to continue...");
    }

    private static void analyzeThreadPoolPerformance() {
        ConsoleUtils.printInfo("Thread pool performance analysis would be integrated with actual simulation data");
        ConsoleUtils.waitForUser("Press Enter to continue...");
    }

    private static void analyzeGarbageCollection() {
        ConsoleUtils.printInfo("GC analysis would show collection frequency, pause times, and memory patterns");
        ConsoleUtils.waitForUser("Press Enter to continue...");
    }

    private static void monitorSystemResources() {
        ConsoleUtils.printInfo("System resource monitoring would display CPU, memory, and I/O metrics");
        ConsoleUtils.waitForUser("Press Enter to continue...");
    }

    private static void configureSettings(int settingType) {
        ConsoleUtils.printInfo("Configuration option " + settingType + " selected");
        ConsoleUtils.printInfo("Settings would be persisted and applied to future simulations");
        ConsoleUtils.waitForUser("Press Enter to continue...");
    }

    private static void handleGracefulExit() {
        ConsoleUtils.clearScreen();
        ConsoleUtils.printHeader("APPLICATION SHUTDOWN");

        System.out.println("Shutting down ConcurQueue application...");
        System.out.println("• Stopping active simulations");
        System.out.println("• Cleaning up resources");
        System.out.println("• Saving final logs");

        logger.info("User initiated graceful shutdown");

        try {
            TimeUnit.MILLISECONDS.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ConsoleUtils.printSuccess("Application shutdown completed successfully.");
        System.out.println("Thank you for using ConcurQueue!");
    }

    private static void performGlobalCleanup() {
        try {
            logger.info("Performing global cleanup operations");

            System.gc();

            logger.info("Global cleanup completed");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during global cleanup", e);
        }
    }
}
