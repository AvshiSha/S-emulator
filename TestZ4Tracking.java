import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.program.ExpansionResult;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import java.nio.file.Path;

public class TestZ4Tracking {
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

            System.out.println("=== Original Program Instructions ===");
            for (int i = 0; i < program.getInstructions().size(); i++) {
                var inst = program.getInstructions().get(i);
                System.out.println((i + 1) + ": " + inst.getName() + " -> " + inst.getVariable());
            }

            System.out.println("\n=== Testing with x1=2, x2=1 ===");

            // Test original program
            System.out.println("\n--- Original Program ---");
            ProgramExecutor executor0 = new ProgramExecutorImpl(program);
            long result0 = executor0.run(2L, 1L);
            System.out.println("Result: " + result0);
            var state0 = executor0.variableState();
            System.out.println("  z4 = " + state0.getOrDefault(semulator.variable.Variable.of("z4"), 0L));

            // Test expanded program
            System.out.println("\n--- Expanded Program (Degree 1) ---");
            ExpansionResult expansion1 = program.expandToDegree(1);
            SProgram expandedProgram1 = new SProgramImpl("expanded1");
            for (var instruction : expansion1.instructions()) {
                expandedProgram1.addInstruction(instruction);
            }

            ProgramExecutor executor1 = new ProgramExecutorImpl(expandedProgram1);
            long result1 = executor1.run(2L, 1L);
            System.out.println("Result: " + result1);
            var state1 = executor1.variableState();
            System.out.println("  z4 = " + state1.getOrDefault(semulator.variable.Variable.of("z4"), 0L));

            // Let's look for any variable that might contain the value 2 (the expected
            // result)
            System.out.println("\nVariables with value 2:");
            state1.forEach((var, value) -> {
                if (value == 2) {
                    System.out.println("  " + var + " = " + value);
                }
            });

            // Let's also check what variables have the highest values
            System.out.println("\nVariables with highest values:");
            state1.entrySet().stream()
                    .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                    .limit(5)
                    .forEach(entry -> System.out.println("  " + entry.getKey() + " = " + entry.getValue()));

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
