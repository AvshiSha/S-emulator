import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.program.ExpansionResult;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import java.nio.file.Path;

public class TestZ4Debug {
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

            System.out.println("=== Testing with x1=2, x2=1 ===");
            System.out.println("Expected result: 2");

            // Test original program
            System.out.println("\n--- Original Program ---");
            ProgramExecutor executor0 = new ProgramExecutorImpl(program);
            long result0 = executor0.run(2L, 1L);
            System.out.println("Result: " + result0);
            System.out.println("Key variables:");
            var state0 = executor0.variableState();
            System.out.println("  x1 = " + state0.getOrDefault(semulator.variable.Variable.of("x1"), 0L));
            System.out.println("  x2 = " + state0.getOrDefault(semulator.variable.Variable.of("x2"), 0L));
            System.out.println("  y = " + state0.getOrDefault(semulator.variable.Variable.RESULT, 0L));
            System.out.println("  z4 = " + state0.getOrDefault(semulator.variable.Variable.of("z4"), 0L));

            // Test expanded program
            System.out.println("\n--- Expanded Program (Degree 1) ---");
            ExpansionResult expansion1 = program.expandToDegree(1);
            SProgram expandedProgram1 = new SProgramImpl("expanded1");
            for (var instruction : expansion1.instructions()) {
                expandedProgram1.addInstruction(instruction);
            }
            if (expandedProgram1 instanceof SProgramImpl programImpl) {
                programImpl.copyFunctionsFrom(program);
            }
            
            ProgramExecutor executor1 = new ProgramExecutorImpl(expandedProgram1);
            long result1 = executor1.run(2L, 1L);
            System.out.println("Result: " + result1);
            System.out.println("Key variables:");
            var state1 = executor1.variableState();
            System.out.println("  x1 = " + state1.getOrDefault(semulator.variable.Variable.of("x1"), 0L));
            System.out.println("  x2 = " + state1.getOrDefault(semulator.variable.Variable.of("x2"), 0L));
            System.out.println("  y = " + state1.getOrDefault(semulator.variable.Variable.RESULT, 0L));
            System.out.println("  z4 = " + state1.getOrDefault(semulator.variable.Variable.of("z4"), 0L));

            // Let's look for variables that might contain the result
            System.out.println("\nAll variables in expanded program:");
            state1.forEach((var, value) -> {
                if (value > 0) {
                    System.out.println("  " + var + " = " + value);
                }
            });

            // Check the last few instructions to see what's happening
            System.out.println("\n--- Last 15 instructions in expanded program ---");
            int start = Math.max(0, expansion1.instructions().size() - 15);
            for (int i = start; i < expansion1.instructions().size(); i++) {
                var inst = expansion1.instructions().get(i);
                System.out.println((i+1) + ": " + inst.getName() + " -> " + inst.getVariable());
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
