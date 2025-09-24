import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import java.nio.file.Path;

public class TestDivide {
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

            // Test execution with inputs: x1 = 10, x2 = 3
            // Expected: 10 / 3 = 3 (integer division)
            System.out.println("\n=== Starting execution with x1 = 10, x2 = 3 ===");
            System.out.println("Expected: 10 / 3 = 3");

            ProgramExecutor executor = new ProgramExecutorImpl(program);

            // Add a timeout to prevent infinite loop
            long startTime = System.currentTimeMillis();
            long resultValue = executor.run(10L, 3L); // x1 = 10, x2 = 3
            long endTime = System.currentTimeMillis();

            System.out.println("\n=== Execution completed ===");
            System.out.println("Final result: " + resultValue);
            System.out.println("Total cycles: " + executor.getTotalCycles());
            System.out.println("Execution time: " + (endTime - startTime) + " ms");
            System.out.println("Expected result: 3");

            if (resultValue == 3) {
                System.out.println("✅ TEST PASSED!");
            } else {
                System.out.println("❌ TEST FAILED!");
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
