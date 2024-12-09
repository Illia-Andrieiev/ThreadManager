package homework;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class WorkerThread extends Thread {
    private final int threadNumber;
    private final WorkerFunction function;
    private final Consumer<Double> resultConsumer; // Consumer for the result
    private Double res = null;
    private int additionalTime;
    private int timeLimit;
    private volatile boolean exceededTimeLimit = false;
    private long startTime;
    private long endTime;

    // Constructor to initialize the WorkerThread with a specific thread number, computing function, and result consumer
    public WorkerThread(int threadNumber, WorkerFunction function, Consumer<Double> resultConsumer) {
        this.threadNumber = threadNumber;
        this.function = function;
        this.resultConsumer = resultConsumer;
        this.additionalTime = 0;
        this.timeLimit = 0;
    }

    // Setter for additional time
    public void setAdditionalTime(int additionalTime) {
        this.additionalTime = additionalTime;
    }

    // Setter for time limit
    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    @Override
    public void run() {
        Timer timer = new Timer();
        startTime = System.currentTimeMillis(); // Record start time

        try {
            // Schedule task to interrupt the thread if it exceeds time limit
            if (timeLimit > 0) {
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        exceededTimeLimit = true;
                        WorkerThread.this.interrupt();
                        System.out.println("Computation " + threadNumber + " exceeded time limit and was interrupted.");
                    }
                }, timeLimit);
            }

            // Loop to check interruption flag frequently during the sleep period
            while (!exceededTimeLimit && (System.currentTimeMillis() - startTime) < additionalTime) {
                try {
                    Thread.sleep(100);  // Sleep briefly to check the interruption flag frequently
                } catch (InterruptedException e) {
                    if (exceededTimeLimit) {
                        throw e;  // Re-throw if interrupted by the timer
                    }
                    // Manually interrupted
                    System.out.println("Computation " + threadNumber + " was interrupted.");
                    endTime = System.currentTimeMillis(); // Record end time
                    return;
                }
            }

            // Perform computation only if the time limit is not exceeded
            if (!exceededTimeLimit) {
                res = function.compute();
                resultConsumer.accept(res); // Pass the result to the consumer
            }

        } catch (InterruptedException e) {
            // The thread was interrupted
            if (!exceededTimeLimit) {
                System.out.println("Computation " + threadNumber + " was interrupted.");
            }
        } finally {
            timer.cancel(); // Cancel the timer when task is finished or interrupted
            endTime = System.currentTimeMillis(); // Record end time
        }

        // Print the result only if the time limit is not exceeded
        if (!exceededTimeLimit) {
            LogWriter.writeToFile("log.txt","Computation " + threadNumber + " finished with result: " + res);
        }
    }

    // Method to get the summary of the thread's state and result
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Component ").append(threadNumber).append(": ");

        if (this.getState() == Thread.State.NEW) {
            summary.append("has not started yet.");
            return summary.toString();
        } else if (this.getState() == Thread.State.RUNNABLE) {
            summary.append("is currently running.");
        } else if (this.getState() == Thread.State.BLOCKED) {
            summary.append("is currently blocked.");
        } else if (this.getState() == Thread.State.WAITING) {
            summary.append("is currently waiting.");
        } else if (this.getState() == Thread.State.TIMED_WAITING) {
            summary.append("is in timed waiting state, simulating long work.");
        } else if (this.getState() == Thread.State.TERMINATED) {
            summary.append("has terminated.");
        }

        if (exceededTimeLimit) {
            summary.append(" It was interrupted due to exceeding the time limit.");
        } else if (res == null) {
            summary.append(" Component is computing...");
        } else {
            summary.append(" It finished with result: ").append(res);
        }

        if (startTime != 0) {
            summary.append("\n started at: ").append(new java.util.Date(startTime));
        }
        if (endTime != 0) {
            summary.append(", ended at: ").append(new java.util.Date(endTime));
            summary.append(", total duration: ").append(endTime - startTime).append(" milliseconds.");
        }
        return summary.toString();
    }
}

