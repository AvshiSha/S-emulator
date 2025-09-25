import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.program.ExpansionResult;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import java.nio.file.Path;

public class TestSimpleDivide {
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

            // Test with simpler inputs first: x1 = 4, x2 = 2 (should return 2)
            System.out.println("\n=== Testing execution with x1 = 4, x2 = 2 ===");
            System.out.println("Expected result: 2");

            // Test degree 0 (original program)
            System.out.println("\n--- Degree 0 (Original Program) ---");
            ProgramExecutor executor0 = new ProgramExecutorImpl(program);
            long result0 = executor0.run(4L, 2L);
            System.out.println("Result: " + result0 + " (Expected: 2)");
            System.out.println("Cycles: " + executor0.getTotalCycles());

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
            long result1 = executor1.run(4L, 2L);
            System.out.println("Result: " + result1 + " (Expected: 2)");
            System.out.println("Cycles: " + executor1.getTotalCycles());

            // Also test with x1 = 6, x2 = 3
            System.out.println("\n=== Testing execution with x1 = 6, x2 = 3 ===");
            System.out.println("Expected result: 2");

            // Test degree 0
            System.out.println("\n--- Degree 0 (Original Program) ---");
            ProgramExecutor executor0b = new ProgramExecutorImpl(program);
            long result0b = executor0b.run(6L, 3L);
            System.out.println("Result: " + result0b + " (Expected: 2)");

            // Test degree 1
            System.out.println("\n--- Degree 1 (First Expansion) ---");
            ProgramExecutor executor1b = new ProgramExecutorImpl(expandedProgram1);
            long result1b = executor1b.run(6L, 3L);
            System.out.println("Result: " + result1b + " (Expected: 2)");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
