import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.program.ExpansionResult;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import java.nio.file.Path;

public class TestDivideExpansion {
    public static void main(String[] args) {
        try {
            // Load the divide program
            SProgram program = new SProgramImpl("Divide");
            Path xmlPath = Path.of(System.getProperty("user.dir") + "/test_divide.xml");
            String validation = program.validate(xmlPath);

            if (!"Valid".equals(validation)) {
                System.err.println("Validation failed: " + validation);
                return;
            }

            Object result = program.load();
            if (result == null) {
                System.err.println("Failed to load program");
                return;
            }

            System.out.println("=== Divide program loaded successfully! ===");
            System.out.println("Program name: " + program.getName());
            System.out.println("Number of instructions: " + program.getInstructions().size());
            System.out.println("Max degree: " + program.calculateMaxDegree());

            // Test execution with inputs: x1 = 6, x2 = 3
            // Expected: 6 / 3 = 2
            System.out.println("\n=== Testing execution with x1 = 6, x2 = 3 ===");
            System.out.println("Expected result: 2");

            // Test degree 0 (original program)
            System.out.println("\n--- Degree 0 (Original Program) ---");
            ProgramExecutor executor0 = new ProgramExecutorImpl(program);
            long result0 = executor0.run(6L, 3L);
            System.out.println("Result: " + result0 + " (Expected: 2)");
            System.out.println("Cycles: " + executor0.getTotalCycles());
            System.out.println("Variables: " + executor0.variableState());

            // Test degree 1
            System.out.println("\n--- Degree 1 (First Expansion) ---");
            ExpansionResult expansion1 = program.expandToDegree(1);
            System.out.println("Expanded instructions count: " + expansion1.instructions().size());

            // Create program from expansion
            SProgram expandedProgram1 = new SProgramImpl("expanded1");
            for (var instruction : expansion1.instructions()) {
                expandedProgram1.addInstruction(instruction);
            }

            ProgramExecutor executor1 = new ProgramExecutorImpl(expandedProgram1);
            long result1 = executor1.run(6L, 3L);
            System.out.println("Result: " + result1 + " (Expected: 2)");
            System.out.println("Cycles: " + executor1.getTotalCycles());
            System.out.println("Variables: " + executor1.variableState());

            // Test degree 2
            System.out.println("\n--- Degree 2 (Second Expansion) ---");
            ExpansionResult expansion2 = program.expandToDegree(2);
            System.out.println("Expanded instructions count: " + expansion2.instructions().size());

            // Create program from expansion
            SProgram expandedProgram2 = new SProgramImpl("expanded2");
            for (var instruction : expansion2.instructions()) {
                expandedProgram2.addInstruction(instruction);
            }

            ProgramExecutor executor2 = new ProgramExecutorImpl(expandedProgram2);
            long result2 = executor2.run(6L, 3L);
            System.out.println("Result: " + result2 + " (Expected: 2)");
            System.out.println("Cycles: " + executor2.getTotalCycles());
            System.out.println("Variables: " + executor2.variableState());

            // Print first few instructions of each expansion to see what's happening
            System.out.println("\n=== Instruction Comparison ===");
            System.out.println("Degree 0 instructions:");
            for (int i = 0; i < Math.min(5, program.getInstructions().size()); i++) {
                System.out.println("  " + (i + 1) + ": " + program.getInstructions().get(i));
            }

            System.out.println("\nDegree 1 instructions:");
            for (int i = 0; i < Math.min(5, expansion1.instructions().size()); i++) {
                System.out.println("  " + (i + 1) + ": " + expansion1.instructions().get(i));
            }

            System.out.println("\nDegree 2 instructions:");
            for (int i = 0; i < Math.min(5, expansion2.instructions().size()); i++) {
                System.out.println("  " + (i + 1) + ": " + expansion2.instructions().get(i));
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
