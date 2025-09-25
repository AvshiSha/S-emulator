import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.program.ExpansionResult;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import semulator.instructions.SInstruction;
import semulator.instructions.AssignVariableInstruction;
import semulator.instructions.AssignConstantInstruction;
import semulator.instructions.QuoteInstruction;
import semulator.instructions.FunctionArgument;
import semulator.instructions.VariableArgument;
import semulator.variable.Variable;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class TestConst3Expansion {
    public static void main(String[] args) {
        try {
            // Create a program that tests the CONST-3 function expansion
            SProgram program = new SProgramImpl("Const3Test");

            // Create the CONST-3 function: y <- 3
            List<SInstruction> const3Function = new ArrayList<>();
            const3Function.add(new AssignConstantInstruction(Variable.RESULT, 3));

            // Add the function to the program
            if (program instanceof SProgramImpl programImpl) {
                // We need to access the functions somehow
            }

            // Main program: z1 <- CONST-3()
            List<FunctionArgument> functionArgs = new ArrayList<>();
            // CONST-3 takes no arguments
            program.addInstruction(
                    new QuoteInstruction(Variable.of("z1"), "CONST-3", functionArgs, const3Function, null));

            System.out.println("=== CONST-3 Expansion Test ===");

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
