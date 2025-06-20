# ConcurQueue: Multithreaded Job Processing Platform

This repository contains the `ConcurQueue` application, a comprehensive Java-based demonstration of concurrent programming concepts, including multithreaded task processing, synchronization, error handling with retries, and system monitoring.

---

## 🚀 Features

- **Task Management:** Defines tasks with unique IDs, names, payloads, and priorities.
- **Producer Threads:** Multiple producers generate tasks of varying priorities into a shared queue.
- **Worker Pool:** A fixed thread pool of workers consumes tasks from the queue concurrently.
- **Priority Queue:** Utilizes a `PriorityBlockingQueue` to ensure higher-priority tasks are processed first.
- **Bounded Queue:** The queue has a fixed capacity to demonstrate backpressure handling and resource limits.
- **Task Status Tracking:** A `ConcurrentHashMap` maintains the real-time status (Submitted, Processing, Completed, Failed-Retry, Failed-Permanently) of every task.
- **Retry Mechanism:** Tasks that fail during processing can be re-queued for a configurable number of retries before being marked as permanently failed.
- **System Monitoring:** A dedicated monitor thread provides real-time insights into queue size, worker pool activity, and task status distribution.
- **Performance Metrics:** Calculates and displays average task processing time and overall system throughput.
- **JSON Export:** Periodically exports all task statuses to a JSON file for external analysis.
- **Deadlock Demonstration:** An isolated simulation to illustrate classic deadlock scenarios.
- **Race Condition Demo:** Highlights common race conditions and demonstrates solutions using atomic operations.
- **Interactive Console UI:** A user-friendly command-line interface to select and run different simulations.
- **Custom Logging:** Configured `java.util.logging` with a custom formatter for colored console output.

---

## 📐 Design & Architecture

The `ConcurQueue` application is structured into several packages, each responsible for a specific aspect of the system:

- `com.tech`: The main package containing the `ConcurQueueApp` (the interactive console entry point).
- `com.tech.model`: Defines core data structures like `Task`, `TaskStatus` (enum), and `TaskStatusEntry`.
- `com.tech.producer`: Contains the `TaskProducer` class, responsible for generating and submitting tasks.
- `com.tech.worker`: Houses the `TaskWorker` class, which consumes and processes tasks. This is where the core work, simulated failures, and retry logic occur.
- `com.tech.monitor`: Includes the `SystemMonitor` class, which observes the entire system, gathers metrics, and handles JSON exports.
- `com.tech.deadlock`: Contains classes (`ResourceLock`, `DeadlockRunnableA`, `DeadlockRunnableB`) specifically designed to demonstrate a classic circular deadlock.
- `com.tech.simulation`: Groups the `ConcurQueueSimulations` (for the main task processing flow) and `RaceConditionDemo` classes, which are callable from the interactive menu.
- `com.tech.util`: Provides utility classes like `LoggerSetup` for custom logging and `ConsoleUtils` for enhanced console output (colors, headers, user input).

### Key Concurrency Concepts Demonstrated:

- **Producer-Consumer Pattern:** Illustrated by `TaskProducers` adding to a shared `BlockingQueue` and `TaskWorkers` consuming from it.
- **Thread Pools:** `Executors.newFixedThreadPool` is used to manage worker threads efficiently.
- **Thread Safety & Synchronization:**
    - `PriorityBlockingQueue` handles thread-safe queue operations.
    - `ConcurrentHashMap` provides thread-safe task status tracking.
    - `AtomicInteger` is used to demonstrate thread-safe counting and prevent race conditions.
    - `synchronized` blocks are used in the deadlock demonstration to show locking behavior.
- **Error Handling and Resilience:** The retry mechanism demonstrates how to build fault tolerance into concurrent systems.
- **Monitoring:** The `SystemMonitor` provides crucial observability into a running concurrent system.

---

## 📦 Project Structure

```
ConcurQueue/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── tech/
│                   ├── deadlock/
│                   │   ├── DeadlockRunnableA.java
│                   │   ├── DeadlockRunnableB.java
│                   │   └── ResourceLock.java
│                   ├── model/
│                   │   ├── Task.java
│                   │   ├── TaskStatus.java
│                   │   └── TaskStatusEntry.java
│                   ├── monitor/
│                   │   └── SystemMonitor.java
│                   ├── producer/
│                   │   └── TaskProducer.java
│                   ├── worker/
│                   │   └── TaskWorker.java
│                   ├── simulation/
│                   │   ├── ConcurQueueSimulations.java
│                   │   └── RaceConditionDemo.java
│                   ├── util/
│                   │   ├── ConsoleUtils.java
│                   │   └── LoggerSetup.java
│                   └── ConcurQueueApp.java
└── out/ (will be created after compilation)
└── concurqueue.log (log file generated at runtime)
└── task_statuses.json (JSON export generated at runtime by Full Simulation)
```

---

## ▶️ How to Build and Run

### Prerequisites

- Java Development Kit (JDK) 11 or newer installed.

### Compilation Instructions

1.  **Navigate to the project root directory** in your terminal where the `src` folder is located:
    ```bash
    cd path/to/your/ConcurQueue
    ```
2.  **Create an output directory** for compiled classes:
    ```bash
    mkdir -p out
    ```
3.  **Compile all Java source files:**
    ```bash
    javac -d out src/main/java/com/tech/**/*.java
    ```
    * This command compiles all `.java` files within the `src/main/java/com/tech` hierarchy and places the compiled `.class` files into the `out` directory, preserving the package structure.

### Running the Application

After successful compilation, you can run the main interactive console application:

```bash
java -cp out com.tech.ConcurQueueApp
```

### Interactive Menu Options:

Upon running, you will be presented with an interactive menu:

1.  **🔒 Deadlock Simulation:**
    - Starts two threads that will intentionally deadlock.
    - **Requires manual termination (Ctrl+C)** from your terminal to proceed back to the main menu or exit the application. This is crucial to observe the deadlock state.
2.  **⚡ Race Condition Demo:**
    - Runs a quick demonstration of a shared counter with and without thread-safe mechanisms (`AtomicInteger`).
    - The simulation will complete automatically and return to the main menu.
3.  **🚀 Full ConcurQueue Simulation:**
    - Launches the full system with producers, workers, queue, monitor, and retry logic.
    - Observe the logs for task submissions, processing, completions, retries, and permanent failures.
    - Monitor reports will show queue size, worker pool status, task status distribution (Submitted, Processing, Completed, Failed(Retry), Failed(Perm)), and performance metrics (Avg Processing Time, Throughput).
    - A `task_statuses.json` file will be generated and updated periodically in the application's root directory.
    - The simulation will gracefully shut down after all initial tasks are processed and all poison pills are consumed, returning to the main menu.
4.  **📊 Performance Analysis:**
    - Provides basic memory usage analysis of the JVM.
    - Other analysis options are placeholders for future expansion.
5.  **⚙️ Configuration & Settings:**
    - A placeholder for adjusting simulation parameters (e.g., logging levels, thread pool sizes) if implemented.
6.  **🚪 Exit Application:**
    - Gracefully shuts down the interactive console application.

---

## 📊 Observing the Output

- **Console Output:** The console will display real-time logs with different colors for various log levels (INFO, WARNING, SEVERE), thanks to `LoggerSetup`.
- **`concurqueue.log`:** A detailed log file will be generated in the project root directory, containing all application logs.
- **`task_statuses.json`:** When running the "Full ConcurQueue Simulation," this JSON file will be created/updated with the complete status of all tracked tasks. You can open this file with any text editor to inspect the structured data.

---

## ⏭️ Future Enhancements

- **Dynamic Configuration:** Implement actual logic for changing simulation parameters via the "Configuration & Settings" menu.
- **Advanced Metrics:** Integrate more sophisticated performance metrics and perhaps an in-memory dashboard.
- **GUI:** Develop a graphical user interface for easier interaction and visual monitoring.
- **Dynamic Worker Scaling:** Implement logic for adjusting the number of worker threads based on queue load.
- **Persistence:** Integrate a proper database for long-term storage of task data.
- **Distributed Processing:** Extend the architecture to simulate distributed task processing across multiple nodes.
