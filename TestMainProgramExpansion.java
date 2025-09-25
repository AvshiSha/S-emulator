import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.program.ExpansionResult;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import semulator.instructions.SInstruction;
import semulator.instructions.AssignVariableInstruction;
import semulator.instructions.IncreaseInstruction;
import semulator.variable.Variable;
import java.util.List;
import java.util.Map;

public class TestMainProgramExpansion {
    public static void main(String[] args) {
        try {
            // Test the hypothesis: main program variables are not being properly handled
            // during expansion

            SProgram program = new SProgramImpl("MainProgramExpansion");

            // Create a program that mixes basic and synthetic instructions
            program.addInstruction(new AssignVariableInstruction(Variable.of("z3"), Variable.of("x1"))); // Synthetic
            program.addInstruction(new IncreaseInstruction(Variable.of("z4"))); // Basic
            program.addInstruction(new AssignVariableInstruction(Variable.RESULT, Variable.of("z4"))); // Synthetic

            System.out.println("=== Main Program Expansion Test ===");

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

            // Show the expanded instructions to see what variables are being used
            System.out.println("\nExpanded instructions:");
            for (int i = 0; i < expansion1.instructions().size(); i++) {
                var inst = expansion1.instructions().get(i);
                System.out.println((i + 1) + ": " + inst.getName() + " -> " + inst.getVariable());
            }

            SProgram expandedProgram1 = new SProgramImpl("expanded1");
            for (var instruction : expansion1.instructions()) {
                expandedProgram1.addInstruction(instruction);
            }

            ProgramExecutor executor1 = new ProgramExecutorImpl(expandedProgram1);
            long result1 = executor1.run(4L, 2L);
            System.out.println("\nResult: " + result1);
            Map<Variable, Long> state1 = executor1.variableState();
            System.out.println("Variables:");
            state1.forEach((var, value) -> System.out.println("  " + var + " = " + value));

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
