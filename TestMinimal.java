import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.program.ExpansionResult;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import java.nio.file.Path;

public class TestMinimal {
    public static void main(String[] args) {
        try {
            // Test with a very simple program first
            System.out.println("=== Testing with original divide program ===");

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

            // Test with x1=2, x2=1 (simple case)
            System.out.println("\n--- Testing x1=2, x2=1 ---");
            System.out.println("Expected result: 2");

            // Original program
            ProgramExecutor executor0 = new ProgramExecutorImpl(program);
            long result0 = executor0.run(2L, 1L);
            System.out.println("Original program result: " + result0 + " (Expected: 2)");
            System.out.println("Cycles: " + executor0.getTotalCycles());

            // Expanded program
            ExpansionResult expansion1 = program.expandToDegree(1);
            System.out.println("Expanded instructions count: " + expansion1.instructions().size());

            SProgram expandedProgram1 = new SProgramImpl("expanded1");
            for (var instruction : expansion1.instructions()) {
                expandedProgram1.addInstruction(instruction);
            }
            if (expandedProgram1 instanceof SProgramImpl programImpl) {
                programImpl.copyFunctionsFrom(program);
            }

            ProgramExecutor executor1 = new ProgramExecutorImpl(expandedProgram1);
            long result1 = executor1.run(2L, 1L);
            System.out.println("Expanded program result: " + result1 + " (Expected: 2)");
            System.out.println("Cycles: " + executor1.getTotalCycles());

            // Check variable states
            System.out.println("\nOriginal program variables:");
            executor0.variableState().forEach((var, value) -> {
                System.out.println("  " + var + " = " + value);
            });

            System.out.println("\nExpanded program variables:");
            executor1.variableState().forEach((var, value) -> {
                System.out.println("  " + var + " = " + value);
            });

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
