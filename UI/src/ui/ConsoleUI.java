package ui;

import org.xml.sax.SAXException;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import semulator.program.ExpansionResult;
import semulator.program.SProgram;
import semulator.variable.Variable;

import javax.xml.parsers.ParserConfigurationException;
// import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.nio.file.Path;
import java.io.*;

public class ConsoleUI {
    private final SProgram gw;
    private final Scanner sc = new Scanner(System.in);
    private Path loadedXml;
    private final RunHistory runHistory = new RunHistory();

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
                    6) Save state
                    7) Load state
                    8) Exit
                    Choose [1-8]:""");
            String ch = sc.nextLine().trim();
            switch (ch) {
                case "1" -> onLoad();
                case "2" -> onShow();
                case "3" -> onExpand();
                case "4" -> onRun();
                case "5" -> onHistory();
                case "6" -> onSaveState();
                case "7" -> onLoadState();
                case "8" -> {
                    System.out.println("Bye.");
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private void onLoad() {
        try {
            System.out.println("Please enter XML Path: ");
            java.util.Scanner sc = new java.util.Scanner(System.in);
            String line = sc.hasNextLine() ? sc.nextLine() : "";
            Path xmlPath = Path.of(line);
            String valid = gw.validate(xmlPath);
            if (!valid.equals("Valid")) {
                System.out.println(valid);
                System.out.println();
                return;
            }

            Object res = gw.load();

            if (res instanceof Path p) {
                loadedXml = p;
                runHistory.clear(); // Clear history when loading a new program
                System.out.println("Loaded successfully.");
                System.out.println();
            } else if (res != null) {
                try {
                    loadedXml = Path.of(res.toString());
                    System.out.println("Mission completed.");
                    System.out.println();
                } catch (Exception e) {
                    System.out.println("Loaded, but could not parse the returned path.");
                    System.out.println();
                }
            } else {
                System.out.println("Load failed.");
                System.out.println();
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
        System.out.println();
        PrettyPrinter.printTopicInputs(gw);
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
            System.out.println("This program is already fully basic (degree 0). Running as-is:");
        } else {
            chosen = askIntInRange("Choose expansion degree [0.." + maxDegree + "]: ", 0, maxDegree);
        }

        // Get input variables from the program
        List<String> inputVars = deriveInputVariables(gw.getInstructions());
        System.out.println("Input variables used: " + String.join(", ", inputVars));

        System.out.println("Enter input values separated by commas (e.g. 5, 0, 12):");
        String inputLine = getValidInput();
        System.out.println();
        List<Long> inputs = new ArrayList<>();
        if (!inputLine.isEmpty()) {
            String[] parts = inputLine.split(",");
            for (String p : parts) {
                try {
                    inputs.add(Long.parseLong(p.trim()));
                } catch (NumberFormatException e) {
                    // If user typed something not numeric, treat as 0
                    inputs.add(0L);
                }
            }
        }

        // Execute the program
        long result;
        int cycles;
        Map<Variable, Long> variableState;

        if (chosen == 0) {
            // Run the original program
            ProgramExecutor executor = new ProgramExecutorImpl(gw);
            result = executor.run(inputs.toArray(new Long[0]));
            cycles = executor.getTotalCycles();
            variableState = executor.variableState();
            System.out.println(PrettyPrinter.show(gw));
        } else {
            // Expand and run
            ExpansionResult snapshot = gw.expandToDegree(chosen);
            // Create a temporary program from the expansion result
            SProgram expandedProgram = createProgramFromExpansion(snapshot);
            ProgramExecutor executor = new ProgramExecutorImpl(expandedProgram);
            result = executor.run(inputs.toArray(new Long[0]));
            cycles = executor.getTotalCycles();
            variableState = executor.variableState();
            System.out.println(PrettyPrinter.show(expandedProgram));
        }

        // Record the run in history
        runHistory.addRun(chosen, inputs, result, cycles);

        // Display results
        System.out.println("\n=== Program Execution Results ===");
        // Display all variables in the required order
        displayVariables(variableState);
        System.out.printf("Total cycles = %d%n", cycles);
        System.out.println();
    }

    private List<String> deriveInputVariables(List<semulator.instructions.SInstruction> instructions) {
        List<String> inputs = new ArrayList<>();
        for (semulator.instructions.SInstruction in : instructions) {
            // Main variable
            if (in.getVariable() != null && in.getVariable().isInput()) {
                String v = in.getVariable().toString();
                if (!inputs.contains(v))
                    inputs.add(v);
            }
            // Source variables for assignments
            if (in instanceof semulator.instructions.AssignVariableInstruction a && a.getSource() != null
                    && a.getSource().isInput()) {
                String s = a.getSource().toString();
                if (!inputs.contains(s))
                    inputs.add(s);
            }
            // Other variables for comparisons
            if (in instanceof semulator.instructions.JumpEqualVariableInstruction j && j.getOther() != null
                    && j.getOther().isInput()) {
                String o = j.getOther().toString();
                if (!inputs.contains(o))
                    inputs.add(o);
            }
        }
        return inputs;
    }

    private SProgram createProgramFromExpansion(ExpansionResult expansion) {
        SProgram program = new semulator.program.SProgramImpl("expanded");
        for (semulator.instructions.SInstruction instruction : expansion.instructions()) {
            program.addInstruction(instruction);
        }
        return program;
    }

    private void displayVariables(Map<Variable, Long> variableState) {
        // Display y first
        Long yValue = variableState.get(Variable.RESULT);
        if (yValue != null) {
            System.out.printf("y = %d%n", yValue);
        }

        // Display x,z variables in order
        List<Variable> xVars = new ArrayList<>();
        List<Variable> zVars = new ArrayList<>();

        for (Map.Entry<Variable, Long> entry : variableState.entrySet()) {
            Variable v = entry.getKey();
            if (v.isInput()) {
                xVars.add(v);
            } else if (v.isWork()) {
                zVars.add(v);
            }
        }

        // Sort x variables by number
        xVars.sort((v1, v2) -> Integer.compare(v1.getNumber(), v2.getNumber()));
        for (Variable xVar : xVars) {
            System.out.printf("%s = %d%n", xVar.toString(), variableState.get(xVar));
        }

        // Sort z variables by number
        zVars.sort((v1, v2) -> Integer.compare(v1.getNumber(), v2.getNumber()));
        for (Variable zVar : zVars) {
            System.out.printf("%s = %d%n", zVar.toString(), variableState.get(zVar));
        }
    }

    private void onHistory() {
        if (runHistory.isEmpty()) {
            System.out.println("First, load and run the program to see the history.");
            return;
        }

        // Calculate dynamic column widths based on actual data
        int maxInputsWidth = calculateMaxInputsWidth();
        int inputsWidth = Math.max(15, Math.min(maxInputsWidth, 50)); // Min 15, Max 50

        System.out.println("\n=== Program Run History ===");

        // Create header with exact spacing
        String header = String.format("%-4s | %-6s | %-" + inputsWidth + "s | %-8s | %-7s",
                "Run", "Level", "Inputs", "Y Value", "Cycles");
        System.out.println(header);

        // Create separator line that matches the header exactly
        String separator = "-".repeat(5) + "|" + "-".repeat(8) + "|" + "-".repeat(inputsWidth + 2) + "|"
                + "-".repeat(10)
                + "|" + "-".repeat(9);
        System.out.println(separator);

        for (RunResult run : runHistory.getAllRuns()) {
            String inputsDisplay = run.inputsCsv();
            if (inputsDisplay.length() > inputsWidth) {
                inputsDisplay = inputsDisplay.substring(0, inputsWidth - 3) + "...";
            }

            System.out.printf("#%-3d | %-6d | %-" + inputsWidth + "s | %-8d | %-7d%n",
                    run.runNumber(),
                    run.level(),
                    inputsDisplay,
                    run.yValue(),
                    run.cycles());
        }
        System.out.println();
    }

    private int calculateMaxInputsWidth() {
        int maxWidth = 0;
        for (RunResult run : runHistory.getAllRuns()) {
            int width = run.inputsCsv().length();
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        return maxWidth;
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

    // === NEW: input validation for comma-separated numbers ===
    private String getValidInput() {
        while (true) {
            String inputLine = sc.nextLine().trim();

            // Check if input is empty
            if (inputLine.isEmpty()) {
                System.out.println("Error: Input cannot be empty. Please enter numbers separated by commas.");
                System.out.println("Enter input values separated by commas (e.g. 5, 0, 12):");
                continue;
            }

            // Split by comma and validate each part
            String[] parts = inputLine.split(",");
            boolean isValid = true;

            for (String part : parts) {
                String trimmedPart = part.trim();

                // Skip empty parts (multiple commas)
                if (trimmedPart.isEmpty()) {
                    continue;
                }

                // Check if the part is a valid number
                try {
                    Long.parseLong(trimmedPart);
                } catch (NumberFormatException e) {
                    System.out.println("Error: '" + trimmedPart + "' is not a valid number.");
                    isValid = false;
                    break;
                }
            }

            if (isValid) {
                return inputLine;
            } else {
                System.out.println("Please enter valid numbers separated by commas (e.g. 5, 0, 12):");
            }
        }
    }

    private void onSaveState() {
        if (loadedXml == null) {
            System.out.println("No program loaded yet.");
            return;
        }

        System.out.println("Enter full path (without extension) to save state:");
        String pathStr = sc.nextLine().trim();
        if (pathStr.isEmpty()) {
            System.out.println("Invalid path.");
            return;
        }

        try {
            Path savePath = Path.of(pathStr + ".state");

            // Create parent directory if it doesn't exist
            Path parentDir = savePath.getParent();
            if (parentDir != null && !java.nio.file.Files.exists(parentDir)) {
                java.nio.file.Files.createDirectories(parentDir);
            }

            ExerciseState state = new ExerciseState(loadedXml, runHistory.getAllRuns());

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(savePath.toFile()))) {
                oos.writeObject(state);
                System.out.println("State saved successfully to: " + savePath);
            }
        } catch (IOException e) {
            System.out.println("Error saving state: " + e.getMessage());
            System.out.println("Make sure the directory exists and you have write permissions.");
        }
    }

    private void onLoadState() {
        System.out.println("Enter full path (without extension) to load state from:");
        String pathStr = sc.nextLine().trim();
        if (pathStr.isEmpty()) {
            System.out.println("Invalid path.");
            return;
        }

        try {
            Path loadPath = Path.of(pathStr + ".state");
            ExerciseState state;

            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(loadPath.toFile()))) {
                state = (ExerciseState) ois.readObject();
            }

            // Load the XML program
            String valid = gw.validate(state.xmlPath());
            if (!valid.equals("Valid")) {
                System.out.println("Error: " + valid);
                return;
            }

            Object res = gw.load();
            if (res instanceof Path p) {
                loadedXml = p;
            } else if (res != null) {
                loadedXml = Path.of(res.toString());
            } else {
                System.out.println("Failed to load program from saved state.");
                return;
            }

            // Restore run history
            runHistory.clear();
            for (RunResult run : state.runHistory()) {
                runHistory.addRun(run.level(), run.inputs(), run.yValue(), run.cycles());
            }

            System.out.println("State loaded successfully from: " + loadPath);

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error loading state: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
        }
    }
}
