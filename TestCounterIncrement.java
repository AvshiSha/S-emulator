import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.program.ExpansionResult;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import semulator.instructions.SInstruction;
import semulator.instructions.IncreaseInstruction;
import semulator.instructions.AssignVariableInstruction;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import semulator.variable.Variable;

public class TestCounterIncrement {
    public static void main(String[] args) {
        try {
            // Create a program that just increments z4 and assigns to y
            SProgram program = new SProgramImpl("Counter");

            // z4 <- 0 (implicit)
            // z4++ (increment)
            program.addInstruction(new IncreaseInstruction(Variable.of("z4")));
            // y <- z4 (assignment)
            program.addInstruction(new AssignVariableInstruction(Variable.RESULT, Variable.of("z4")));

            System.out.println("=== Counter Increment Test ===");

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

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
