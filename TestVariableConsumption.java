import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.program.ExpansionResult;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import semulator.instructions.SInstruction;
import semulator.instructions.AssignVariableInstruction;
import semulator.instructions.IncreaseInstruction;
import semulator.variable.Variable;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class TestVariableConsumption {
    public static void main(String[] args) {
        try {
            // Test the hypothesis: variables are being consumed during expansion
            // Create a simple test that mimics the divide program structure

            SProgram program = new SProgramImpl("VariableConsumption");

            // Simulate the problematic sequence from divide program:
            // 1. z3 <- x1 (assignment)
            program.addInstruction(new AssignVariableInstruction(Variable.of("z3"), Variable.of("x1")));

            // 2. z4++ (increment counter)
            program.addInstruction(new IncreaseInstruction(Variable.of("z4")));

            // 3. y <- z4 (final assignment - this should work)
            program.addInstruction(new AssignVariableInstruction(Variable.RESULT, Variable.of("z4")));

            System.out.println("=== Variable Consumption Test ===");

            // Test degree 0
            System.out.println("\n--- Degree 0 ---");
            ProgramExecutor executor0 = new ProgramExecutorImpl(program);
            long result0 = executor0.run(4L, 2L);
            System.out.println("Result: " + result0);
            Map<Variable, Long> state0 = executor0.variableState();
            System.out.println("Variables:");
            state0.forEach((var, value) -> System.out.println("  " + var + " = " + value));

            // Test degree 1
            System.out.println("\n--- Degree 1 ---");
            ExpansionResult expansion1 = program.expandToDegree(1);
            System.out.println("Expanded instructions count: " + expansion1.instructions().size());

            SProgram expandedProgram1 = new SProgramImpl("expanded1");
            for (var instruction : expansion1.instructions()) {
                expandedProgram1.addInstruction(instruction);
            }

            ProgramExecutor executor1 = new ProgramExecutorImpl(expandedProgram1);
            long result1 = executor1.run(4L, 2L);
            System.out.println("Result: " + result1);
            Map<Variable, Long> state1 = executor1.variableState();
            System.out.println("Variables:");
            state1.forEach((var, value) -> System.out.println("  " + var + " = " + value));

            // Check if z3 and z4 are present
            System.out.println("\nVariable existence check:");
            System.out.println("  z3 exists: " + state1.containsKey(Variable.of("z3")));
            System.out.println("  z4 exists: " + state1.containsKey(Variable.of("z4")));
            System.out.println("  y exists: " + state1.containsKey(Variable.RESULT));

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
