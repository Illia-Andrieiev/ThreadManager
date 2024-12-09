/*
* command for compile program from homework directory:
* javac -d classes Main.java WorkerThread.java Reader.java WorkerFunction.java ComputingFunctions.java LogWriter.java
* command for run program from homework directory:
* java -cp classes homework.Main
*/

package homework;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
    private static Pipe pipe;
    private static Map<Integer, Map<Integer, Double>> results = new HashMap<>();
    private static int currentGroupNumber = 0;

    public static void main(String[] args) {
        // Use Pipe for communication
        try {
            pipe = Pipe.open();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Initialize Reader with a Pipe.SourceChannel and a Consumer to process group results
        Reader reader = new Reader(pipe.source(), groupResults -> {
            results.put(currentGroupNumber, new HashMap<>(groupResults));
            currentGroupNumber++;
        });
        Thread readerThread = new Thread(reader);
        readerThread.start();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Run <help> to see available commands. Check \"log.txt\" to trace components work");
        while (true) {
            // Scan commands
            String command = scanner.nextLine();
            command = command.trim();
            String[] commandParts = command.split("\\s+");

            if (commandParts.length == 1 &&
                    (commandParts[0].equals("-r") || commandParts[0].equals("run"))) {
                sendCommand("run");
            } else if (commandParts.length == 1 &&
                    (commandParts[0].equals("help") || commandParts[0].equals("-h"))) {
                System.out.println("""
                        available commands:
                        *  help, -h: print help message;
                        *  group, -g <x> [limit, -l <time>]: create new group with x = <x>. <x> must be int.
                                   [limit, -l]: optional attribute for working time limit for all group components,
                                    after which it`s will be canceled. As value takes INT number of milliseconds.
                                    By default unlimited. Time limit settled specifically for component more prior.
                        *  new <componentSymbol> [<attribute> <value>]*: create new computing thread into group with
                        // operation <componentSymbol>. Available symbols:
                             squareRoot, --sr: find square root of x. x must be >= 0.
                             square, -s: find x*x.
                             factorial, -f: find x!. x must be > 0.
                             abs, -a: find abs(x).
                             duplicate, -d: find 2*x.
                        // [<attribute> <value>] it`s not necessary parameters. Available attributes:
                             limit, -l: working time limit for component, after which it`s will be canceled. More prior then group limit.
                                                    As value takes INT number of milliseconds. By default unlimited.
                             additional_time, --at: additional working time for component.
                                                    Helps in creating different situations. By default = 0.
                                                    As value takes INT number of milliseconds.
                           Command examples:
                             new abs -l 2000
                             new --sr --at 1500
                             new -f
                             new -d --at 1800 limit 1300
                        *  run, -r: run group computing components.

                        !!! EACH COMPONENT RUNS ONLY ONCE, AND AUTOMATICALLY DELETED BEFORE NEW RUN !!!

                        *  summary, -s: print current status of all components.
                        *  exit, -e: terminate program.
                        *  cancel, -c <computation_element>: cancel computation element, that can be
                             -g: cancel all group. Interchangeable with command <clear>
                             -C <index> cancel particular component. Require component index.
                        """);
            } else if (commandParts.length > 1 &&
                    (commandParts[0].equals("cancel") || commandParts[0].equals("-c"))) {
                if (commandParts[1].equals("-g")) {
                    sendCommand("cancel group");
                } else if (commandParts[1].equals("-C")) {
                    if (commandParts.length == 2) {
                        System.out.println("Error: cancellation of computing component requires an index. Run <help> to see more.");
                    } else {
                        sendCommand("cancel component " + Integer.parseInt(commandParts[2]));
                    }
                }
            } else if (commandParts.length == 1 &&
                    (commandParts[0].equals("exit") || commandParts[0].equals("-e"))) {
                sendCommand("clear");
                break;
            } else if ((commandParts.length == 2 || commandParts.length == 4) &&
                    (commandParts[0].equals("group") || commandParts[0].equals("-g"))) {
                int timeLimit = 0;
                if (commandParts.length == 4) {
                    if (commandParts[2].equals("limit") || commandParts[2].equals("-l")) {
                        timeLimit = Integer.parseInt(commandParts[3]);
                    } else {
                        System.out.println("Error: incorrect command attributes. Run <help> to see available command attributes. All entered attributes will be ignored.");
                    }
                }
                try {
                    int x = Integer.parseInt(commandParts[1]);
                    if (timeLimit < 1)
                        sendCommand("group " + x);
                    else {
                        sendCommand("group " + x + " limit " + timeLimit);
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Error: x must be an integer.");
                }
            } else if (commandParts.length > 1 && commandParts.length % 2 == 0 && commandParts[0].equals("new")) {
                Map<String, String> commandPairs = extractCommands(commandParts);
                String readerCommand = "";
                switch (commandParts[1]) {
                    case "squareRoot":
                    case "--sr":
                        readerCommand = "new squareRoot";
                        readerCommand = updateReaderCommand(readerCommand, commandPairs);
                        sendCommand(readerCommand);
                        break;
                    case "square":
                    case "-s":
                        readerCommand = "new square";
                        readerCommand = updateReaderCommand(readerCommand, commandPairs);
                        sendCommand(readerCommand);
                        break;
                    case "factorial":
                    case "-f":
                        readerCommand = "new factorial";
                        readerCommand = updateReaderCommand(readerCommand, commandPairs);
                        sendCommand(readerCommand);
                        break;
                    case "abs":
                    case "-a":
                        readerCommand = "new abs";
                        readerCommand = updateReaderCommand(readerCommand, commandPairs);
                        sendCommand(readerCommand);
                        break;
                    case "duplicate":
                    case "-d":
                        readerCommand = "new duplicate";
                        readerCommand = updateReaderCommand(readerCommand, commandPairs);
                        sendCommand(readerCommand);
                        break;
                    default:
                        System.out.println("Error: incorrect <componentSymbol>. Run <help> to see available commands.");
                }
            } else if (commandParts.length == 1 &&
                    (commandParts[0].equals("summary") || commandParts[0].equals("-s"))) {
                sendCommand("summary");
            } else {
                System.out.println("Error: incorrect command. Run <help> to see available commands.");
            }
        }
        scanner.close();
        // Stop Reader and wait for completion
        reader.stop();
        try {
            readerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Program finished. Results for each group run:");
        System.out.println(results);
    }

    // Extract command key-value pairs from command parts
    private static Map<String, String> extractCommands(String[] commandParts) {
        HashMap<String, String> commandPairs = new HashMap<>();
        for (int i = 0; i < commandParts.length; i += 2) {
            commandPairs.put(commandParts[i], commandParts[i + 1]);
        }
        return commandPairs;
    }

    // Update reader command with extracted attributes
    private static String updateReaderCommand(String readerCommand, Map<String, String> commandPairs) {
        int timeLimit = -1;
        int additionWorkingTime = 0;
        boolean isCorrectAttributes = true;
        for (Map.Entry<String, String> entry : commandPairs.entrySet()) {
            switch (entry.getKey()) {
                case "limit":
                case "-l":
                    timeLimit = Integer.parseInt(entry.getValue());
                    break;
                case "additional_time":
                case "--at":
                    additionWorkingTime = Integer.parseInt(entry.getValue());
                    break;
                case "new":
                    break;
                default:
                    System.out.println("Error: incorrect command attributes. Run <help> to see available command attributes. All entered attributes will be ignored.");
                    isCorrectAttributes = false;
            }
        }
        if (isCorrectAttributes) {
            if (timeLimit > 0) {
                readerCommand = readerCommand + " limit " + timeLimit;
            }
            if (additionWorkingTime > 0) {
                readerCommand = readerCommand + " additional_time " + additionWorkingTime;
            }
        }
        return readerCommand;
    }

    // Send command through Pipe
    private static void sendCommand(String command) {
        try {
            Pipe.SinkChannel sinkChannel = pipe.sink();
            ByteBuffer buffer = ByteBuffer.allocate(256);
            buffer.clear();
            buffer.put(command.getBytes());
            buffer.flip();
            while (buffer.hasRemaining()) {
                sinkChannel.write(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
