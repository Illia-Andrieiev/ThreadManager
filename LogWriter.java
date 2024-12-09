package homework;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class LogWriter {
    private static final Object lock = new Object();

    public static void writeToFile(String log, String message) {
        synchronized (lock) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(log, true))) {
                writer.write(message);
                writer.newLine();
            } catch (IOException e) {
                System.err.println("Error writing to file: " + e.getMessage());
            }
        }
    }
}

