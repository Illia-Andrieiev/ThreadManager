package homework;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


public class Reader implements Runnable {
    private final Pipe.SourceChannel sourceChannel;
    private final Map<Integer, WorkerThread> threads = new HashMap<>();
    private final ComputingFunctions computingCore;
    private volatile boolean running = true;
    private int groupTimeLimit = 0;
    private final Map<Integer, Double> results = new ConcurrentHashMap<>(); // Store results here
    private final Consumer<Map<Integer, Double>> resultConsumer;
    // Constructor to initialize the source channel and computing functions
    public Reader(Pipe.SourceChannel sourceChannel, Consumer<Map<Integer, Double>> resultConsumer) {
        this.sourceChannel = sourceChannel;
        this.computingCore = new ComputingFunctions(1);
        this.resultConsumer = resultConsumer;
    }

    @Override
    public void run() {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        try {
            while (running) {
                buffer.clear();
                int bytesRead = sourceChannel.read(buffer); // Blocking query
                if (bytesRead > 0) {
                    // Read command
                    buffer.flip();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    String command = new String(bytes);
                    if (command.startsWith("new")) {
                        // Handle the creation of a new WorkerThread
                        int numberOfThread = 0;
                        String[] parts = command.split(" ");
                        String type = parts[1];
                        int timeLimit = -1;
                        int additionalTime = 0;

                        // Extract attributes
                        for (int i = 2; i < parts.length; i += 2) {
                            switch (parts[i]) {
                                case "limit":
                                    timeLimit = Integer.parseInt(parts[i + 1]);
                                    break;
                                case "additional_time":
                                    additionalTime = Integer.parseInt(parts[i + 1]);
                                    break;
                            }
                        }

                        // Find an available thread number
                        while (threads.containsKey(numberOfThread)) {
                            numberOfThread++;
                        }
                        WorkerThread thread;
                        int finalNumberOfThread = numberOfThread;
                        switch (type) {
                            case "squareRoot":
                                thread = new WorkerThread(numberOfThread, computingCore::squareRoot, result -> results.put(finalNumberOfThread, result));
                                if (timeLimit > 0) {
                                    thread.setTimeLimit(timeLimit);
                                } else if(groupTimeLimit != 0){
                                    thread.setTimeLimit(groupTimeLimit);
                                }
                                if (additionalTime > 0) { thread.setAdditionalTime(additionalTime); }
                                threads.put(numberOfThread, thread);
                                System.out.println("Computing component squareRoot" +
                                        " with index " + numberOfThread + " added to group.");
                                break;
                            case "square":
                                thread = new WorkerThread(numberOfThread, computingCore::square, result -> results.put(finalNumberOfThread, result));
                                if (timeLimit > 0) {
                                    thread.setTimeLimit(timeLimit);
                                } else if(groupTimeLimit != 0){
                                    thread.setTimeLimit(groupTimeLimit);
                                }                                if (additionalTime > 0) { thread.setAdditionalTime(additionalTime); }
                                threads.put(numberOfThread, thread);
                                System.out.println("Computing component square" +
                                        " with index " + numberOfThread + " added to group.");
                                break;
                            case "factorial":
                                thread = new WorkerThread(numberOfThread, computingCore::factorial, result -> results.put(finalNumberOfThread, result));
                                if (timeLimit > 0) {
                                    thread.setTimeLimit(timeLimit);
                                } else if(groupTimeLimit != 0){
                                    thread.setTimeLimit(groupTimeLimit);
                                }                                if (additionalTime > 0) { thread.setAdditionalTime(additionalTime); }
                                threads.put(numberOfThread, thread);
                                System.out.println("Computing component factorial" +
                                        " with index " + numberOfThread + " added to group.");
                                break;
                            case "abs":
                                thread = new WorkerThread(numberOfThread, computingCore::abs, result -> results.put(finalNumberOfThread, result));
                                if (timeLimit > 0) {
                                    thread.setTimeLimit(timeLimit);
                                } else if(groupTimeLimit != 0){
                                    thread.setTimeLimit(groupTimeLimit);
                                }                                if (additionalTime > 0) { thread.setAdditionalTime(additionalTime); }
                                threads.put(numberOfThread, thread);
                                System.out.println("Computing component abs" +
                                        " with index " + numberOfThread + " added to group.");
                                break;
                            case "duplicate":
                                thread = new WorkerThread(numberOfThread, computingCore::duplicate, result -> results.put(finalNumberOfThread, result));
                                if (timeLimit > 0) {
                                    thread.setTimeLimit(timeLimit);
                                } else if(groupTimeLimit != 0){
                                    thread.setTimeLimit(groupTimeLimit);
                                }                                if (additionalTime > 0) { thread.setAdditionalTime(additionalTime); }
                                threads.put(numberOfThread, thread);
                                System.out.println("Computing component duplicate" +
                                        " with index " + numberOfThread + " added to group.");
                                break;
                        }
                    } else if (command.equals("summary")) {
                        // Print the summary of all worker threads
                        printSummary();
                    } else if (command.equals("run")) {
                        // Create a monitoring thread to check the completion of all worker threads
                        new Thread(this::monitorCompletion).start();

                        // Creating a list of keys to remove invalid threads
                        List<Integer> keysToRemove = new ArrayList<>();

                        for (Map.Entry<Integer, WorkerThread> entry : threads.entrySet()) {
                            WorkerThread thread = entry.getValue();

                            // Check current thread state
                            if (thread.getState() == Thread.State.NEW) {
                                thread.start();
                            } else {
                                // If the thread is not in the NEW state, add its key to the list for deletion
                                keysToRemove.add(entry.getKey());
                            }
                        }

                        // Remove invalid threads
                        for (Integer key : keysToRemove) {
                            threads.remove(key);
                        }
                    } else if (command.startsWith("group")) {
                        // Set a new value of x for the computing core
                        stopAllThreads();
                        threads.clear();
                        String[] commandParts = command.split(" ");
                        int x = Integer.parseInt(commandParts[1]);
                        if(commandParts.length > 2 && commandParts[2].equals("limit")){
                            groupTimeLimit = Integer.parseInt(commandParts[3]);
                            System.out.println("New group submitted with x = " + x +
                                    " and group time limit = " + groupTimeLimit);
                        }else{
                            groupTimeLimit = 0;
                            System.out.println("New group submitted with x = " + x);
                        }
                        computingCore.setX(x);
                    } else if (command.startsWith("cancel")) {
                        if (command.split(" ")[1].equals("group")) {
                            // Cancel all worker threads
                            stopAllThreads();
                            System.out.println("All group components were canceled");
                            threads.clear();
                        } else if (command.split(" ")[1].equals("component")) {
                            // Cancel a specific worker thread
                            int key = Integer.parseInt(command.split(" ")[2]);
                            WorkerThread thread = threads.remove(key);
                            if (thread != null) {
                                thread.interrupt();
                                System.out.println("Component with index " + key + " canceled.");
                            } else {
                                System.out.println("Component with index " + key + " not found.");
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to stop all worker threads
    private void stopAllThreads() {
        for (Map.Entry<Integer, WorkerThread> entry : threads.entrySet())
            entry.getValue().interrupt();
    }

    // Method to print the summary of all worker threads
    private void printSummary() {
        if (threads.isEmpty()) {
            System.out.println("No components settled");
        }
        for (Map.Entry<Integer, WorkerThread> entry : threads.entrySet())
            System.out.println(entry.getValue().getSummary());
    }

    // Method to stop the reader
    public void stop() {
        running = false;
    }
    // Monitor the completion of all worker threads
    private void monitorCompletion() {
        while (true) {
            boolean allFinished = true;
            for (WorkerThread thread : threads.values()) {
                if (thread.isAlive()) {
                    allFinished = false;
                    break;
                }
            }
            if (allFinished) {
                System.out.println("All worker threads have finished their work.");
                // Send to Main group results
                resultConsumer.accept(new HashMap<>(results));
                break;
            }
            try {
                Thread.sleep(100); // Sleep briefly to avoid busy-waiting
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
}
}
