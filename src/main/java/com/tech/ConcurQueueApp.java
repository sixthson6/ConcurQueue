package com.tech;

import com.tech.simulation.ConcurQueueSimulations;
import com.tech.simulation.RaceConditionDemo;
import com.tech.util.LoggerSetup;

import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConcurQueueApp {

    private static final Logger logger = Logger.getLogger(ConcurQueueApp.class.getName());

    public static void main(String[] args) {
        LoggerSetup.setup();
        logger.info("Welcome to ConcurQueue - Multithreaded Job Processing Platform!");

        Scanner scanner = new Scanner(System.in);
        int choice = -1;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Global Shutdown Hook activated. Performing final cleanup...");
            logger.info("Global Shutdown Hook finished.");
        }, "GlobalShutdownHook"));


        while (choice != 0) {
            System.out.println("\n______________________________________________________________________________");
            System.out.println("Please choose a simulation to run:");
            System.out.println("1. Show Deadlock Simulation (Will require Ctrl+C to terminate)");
            System.out.println("2. Show Race Conditions (Will run and show results)");
            System.out.println("3. Show Full ConcurQueue Simulation (Comprehensive demo with monitor & retries)");
            System.out.println("0. Exit Application");
            System.out.println("______________________________________________________________________________");
            System.out.print("Enter your choice: ");

            try {
                choice = Integer.parseInt(scanner.nextLine());

                switch (choice) {
                    case 1:
                        ConcurQueueSimulations.runDeadlockSimulation();
                        break;
                    case 2:
                        RaceConditionDemo.runSimulation();
                        break;
                    case 3:
                        ConcurQueueSimulations.runFullSimulation();
                        break;
                    case 0:
                        logger.info("Exiting application. Goodbye!");
                        break;
                    default:
                        System.out.println("Invalid choice. Please enter a number between 0 and 3.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                logger.log(Level.WARNING, "Invalid number format input.", e);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "An unexpected error occurred in the main application loop.", e);
                System.out.println("An unexpected error occurred: " + e.getMessage());
            }
        }
        scanner.close();
    }
}
