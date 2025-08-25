package ui;

import org.xml.sax.SAXException;
import semulator.program.ExpansionResult;
import semulator.program.SProgram;

import javax.xml.parsers.ParserConfigurationException;
// import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.nio.file.Path;

public class ConsoleUI {
    private final SProgram gw;
    private final Scanner sc = new Scanner(System.in);
    private Path loadedXml;

    public ConsoleUI(SProgram gw) {
        this.gw = gw;
    }

    public void start() throws IOException, ParserConfigurationException, SAXException {
        while (true) {
            System.out.println("""
                    === S-Emulator ===
                    1) Load program
                    2) Show program
                    3) Expand
                    4) Run
                    5) History
                    6) Exit
                    Choose [1-6]:""");
            String ch = sc.nextLine().trim();
            switch (ch) {
                case "1" -> onLoad();
                case "2" -> onShow();
                case "3" -> onExpand();
                case "4" -> onRun();
                case "5" -> onHistory();
                case "6" -> {
                    System.out.println("Bye.");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void onLoad() {
        try {
            boolean valid = gw.validate();
            if (!valid) {
                System.out.println("Invalid program. Try to upload the path again");
                return;
            }

            Object res = gw.load();

            if (res instanceof java.nio.file.Path p) {
                loadedXml = p;
                System.out.println("Mission completed.");
            } else if (res != null) {
                try {
                    loadedXml = java.nio.file.Path.of(res.toString());
                    System.out.println("Mission completed.");
                } catch (Exception e) {
                    System.out.println("Loaded, but could not parse the returned path.");
                }
            } else {
                System.out.println("Load failed.");
            }

        } catch (javax.xml.parsers.ParserConfigurationException | org.xml.sax.SAXException e) {
            System.out.println("XML error: " + e.getMessage());
        } catch (java.io.IOException e) {
            System.out.println("I/O error: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
        }
    }

    private void onShow() {
        if (loadedXml == null) {
            System.out.println("No program loaded yet.");
            return;
        }
        if (gw.getInstructions() == null || gw.getInstructions().isEmpty()) {
            System.out.println("Program loaded but not built yet. (No in-memory instructions)");
            return;
        }
        System.out.println(PrettyPrinter.show(gw));
    }

    private void onExpand() {
        if (loadedXml == null) {
            System.out.println("No program loaded yet.");
            return;
        }
        if (gw.getInstructions() == null || gw.getInstructions().isEmpty()) {
            System.out.println("Program loaded but not built yet. (No in-memory instructions)");
            return;
        }

        int maxDegree = gw.calculateMaxDegree();
        System.out.printf("Max degree: %d%n", maxDegree);

        final int chosen;
        if (maxDegree == 0) {
            // Nothing to expand, but still show the snapshot (degree 0)
            chosen = 0;
            System.out.println("This program is already fully basic (degree 0). Showing as-is:");
        } else {
            chosen = askIntInRange("Choose expansion degree [0.." + maxDegree + "]: ", 0, maxDegree);
        }

        // Engine: expand to the requested degree (and carry lineage)
        ExpansionResult snapshot = gw.expandToDegree(chosen);

        // UI: print including creator chain with "<<<"
        System.out.println();
        System.out.println("Program after expanding to degree " + chosen + ":");
        // System.out.println(PrettyPrinter.showWithCreators(snapshot));
        System.out.println(PrettyPrinter.showRowMajor(snapshot));
    }

    private void onRun() {
        // int level = askInt("Run level (0..): ");
        //System.out.print("Inputs CSV (e.g., 4,7,0): ");
        //var res = gw.run(level, sc.nextLine().trim());
        //System.out.printf("y = %d%nc ycles = %d%n", res.y(), res.cycles());
        //System.out.printf("y = %d%nc ycles = %d%n", res.y(), res.cycles());
    }

    private void onHistory() {
        //List<ProgramGateway.RunResult> hist = gw.history();
        //if (hist.isEmpty()) {
        //System.out.println("No runs yet.");
        //   return;
        //}
        //for (var h : hist) {
        //    System.out.printf("#%d | level=%d | inputs=%s | y=%d | cycles=%d%n",
        //            h.runNo(), h.level(), h.inputsCsv(), h.y(), h.cycles());
        //}
    }

    private int askInt(String prompt, int maxDegree) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    // === NEW: strict numeric + range validation ===
    private int askIntInRange(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try {
                int v = Integer.parseInt(s);
                if (v < min || v > max) {
                    System.out.printf("Error: please enter a number between %d and %d.%n", min, max);
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("Error: please enter a valid integer.");
            }
        }
    }
}
