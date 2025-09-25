import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.program.ExpansionResult;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import semulator.instructions.SInstruction;
import semulator.instructions.QuoteInstruction;
import semulator.instructions.AssignVariableInstruction;
import semulator.instructions.IncreaseInstruction;
import semulator.variable.Variable;
import semulator.instructions.FunctionArgument;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class TestSimpleQuote {
    public static void main(String[] args) {
        try {
            // Create a program that uses a simple QUOTE call
            SProgram program = new SProgramImpl("SimpleQuote");

            // Create a simple function: CONST0 (returns 0)
            List<SInstruction> const0Function = new ArrayList<>();
            // y <- 0 (the function body)
            const0Function.add(new AssignVariableInstruction(Variable.RESULT, Variable.of("x1"))); // This will be
                                                                                                   // wrong, but let's
                                                                                                   // see

            // Add the function to the program
            if (program instanceof SProgramImpl programImpl) {
                // We'll use the existing functions from the divide program
            }

            // Main program: z4 <- CONST0()
            List<FunctionArgument> functionArgs = new ArrayList<>();
            functionArgs.add(FunctionArgument.of(Variable.of("x1")));
            program.addInstruction(
                    new QuoteInstruction(Variable.of("z4"), "CONST0", functionArgs, const0Function, null));

            // y <- z4
            program.addInstruction(new AssignVariableInstruction(Variable.RESULT, Variable.of("z4")));

            System.out.println("=== Simple Quote Test ===");

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
