import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.program.ExpansionResult;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import java.nio.file.Path;

public class TestDetailedDebug {
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

            System.out.println("=== Testing with x1=4, x2=2 ===");

            // Test original program
            System.out.println("\n--- Original Program ---");
            ProgramExecutor executor0 = new ProgramExecutorImpl(program);
            long result0 = executor0.run(4L, 2L);
            System.out.println("Result: " + result0);
            System.out.println("Variable states:");
            executor0.variableState().forEach((var, value) -> {
                System.out.println("  " + var + " = " + value);
            });

            // Test expanded program
            System.out.println("\n--- Expanded Program (Degree 1) ---");
            ExpansionResult expansion1 = program.expandToDegree(1);
            SProgram expandedProgram1 = new SProgramImpl("expanded1");
            for (var instruction : expansion1.instructions()) {
                expandedProgram1.addInstruction(instruction);
            }

            ProgramExecutor executor1 = new ProgramExecutorImpl(expandedProgram1);
            long result1 = executor1.run(4L, 2L);
            System.out.println("Result: " + result1);
            System.out.println("Variable states:");
            executor1.variableState().forEach((var, value) -> {
                System.out.println("  " + var + " = " + value);
            });

            // Let's also check what the last few instructions look like in the expanded
            // program
            System.out.println("\n--- Last 10 instructions in expanded program ---");
            int start = Math.max(0, expansion1.instructions().size() - 10);
            for (int i = start; i < expansion1.instructions().size(); i++) {
                var inst = expansion1.instructions().get(i);
                System.out.println((i + 1) + ": " + inst.getName() + " -> " + inst.getVariable());
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
