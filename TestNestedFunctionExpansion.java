import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.program.ExpansionResult;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import semulator.instructions.SInstruction;
import semulator.instructions.AssignConstantInstruction;
import semulator.instructions.QuoteInstruction;
import semulator.instructions.FunctionArgument;
import semulator.instructions.FunctionCall;
import semulator.variable.Variable;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class TestNestedFunctionExpansion {
    public static void main(String[] args) {
        try {
            // Test nested function expansion by creating a simple nested call
            // This will help us understand if the issue is with nested function handling

            SProgram program = new SProgramImpl("NestedFunctionTest");

            // Create a simple function that calls another function
            // CONST3: y <- 3
            List<SInstruction> const3Function = new ArrayList<>();
            const3Function.add(new AssignConstantInstruction(Variable.RESULT, 3));

            // Add the function to the program
            if (program instanceof SProgramImpl programImpl) {
                // We need to access the functions somehow
                // For now, let's just test the expansion directly
            }

            // Create a nested function call: z1 <- CONST3()
            List<FunctionArgument> args = new ArrayList<>();
            // CONST3 takes no arguments
            program.addInstruction(new QuoteInstruction(Variable.of("z1"), "CONST3", args, const3Function, null));

            System.out.println("=== Nested Function Expansion Test ===");

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

            // Show the expanded instructions
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
