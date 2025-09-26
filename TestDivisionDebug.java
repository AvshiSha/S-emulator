import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import java.nio.file.Path;

public class TestDivisionDebug {
    public static void main(String[] args) {
        try {
            // Create and load the division program
            SProgramImpl program = new SProgramImpl("Divide");
            Path xmlPath = Path.of("test_divide.xml").toAbsolutePath();
            program.validate(xmlPath);
            program.load();

            System.out.println("=== Debugging Division Algorithm ===");
            System.out.println("Input: x1=2, x2=1");
            System.out.println("Expected: y=2 (since 2 ÷ 1 = 2)");
            System.out.println();

            // Test the original program first
            System.out.println("--- Original Program ---");
            ProgramExecutor executor = new ProgramExecutorImpl(program);
            long result = executor.run(2L, 1L);
            System.out.println("Original result: y=" + result);

            // Get the execution state to see variable values
            var executionState = executor.variableState();
            System.out.println("Execution state variables:");
            for (var entry : executionState.entrySet()) {
                System.out.println("  " + entry.getKey() + " = " + entry.getValue());
            }
            System.out.println();

            // Test expanded program at degree 0
            System.out.println("--- Expanded Program (Degree 0) ---");
            var expansionResult = program.expandToDegree(0);
            SProgram expandedProgram = new SProgramImpl("ExpandedDivide");
            for (var instruction : expansionResult.instructions()) {
                expandedProgram.addInstruction(instruction);
            }

            ProgramExecutor expandedExecutor = new ProgramExecutorImpl(expandedProgram);
            long expandedResult = expandedExecutor.run(2L, 1L);
            System.out.println("Expanded result: y=" + expandedResult);

            // Get the execution state to see variable values
            var expandedExecutionState = expandedExecutor.variableState();
            System.out.println("Expanded execution state variables:");
            for (var entry : expandedExecutionState.entrySet()) {
                System.out.println("  " + entry.getKey() + " = " + entry.getValue());
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
