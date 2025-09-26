import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import java.nio.file.Path;

public class TestExpandedDivision {
    public static void main(String[] args) {
        try {
            // Create and load the division program
            SProgramImpl program = new SProgramImpl("Divide");
            Path xmlPath = Path.of("test_divide.xml").toAbsolutePath();
            program.validate(xmlPath);
            program.load();

            System.out.println("=== Testing Expanded Division Algorithm ===");
            System.out.println("Input: x1=2, x2=1");
            System.out.println("Expected: y=2 (since 2 ÷ 1 = 2)");
            System.out.println();

            // Test different expansion degrees
            for (int degree = 0; degree <= 4; degree++) {
                System.out.println("--- Testing Degree " + degree + " ---");

                // Expand to the specified degree
                var expansionResult = program.expandToDegree(degree);
                SProgram expandedProgram = new SProgramImpl("ExpandedDivide");

                // Add all expanded instructions to the new program
                for (var instruction : expansionResult.instructions()) {
                    expandedProgram.addInstruction(instruction);
                }

                // Execute the expanded program
                ProgramExecutor executor = new ProgramExecutorImpl(expandedProgram);
                long result = executor.run(2L, 1L);

                System.out.println("Degree " + degree + " result: y=" + result);

                if (executor instanceof ProgramExecutorImpl) {
                    ProgramExecutorImpl impl = (ProgramExecutorImpl) executor;
                    System.out.println("  Cycles: " + impl.getTotalCycles());
                }
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
