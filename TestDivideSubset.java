import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.program.ExpansionResult;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import semulator.instructions.SInstruction;
import semulator.instructions.AssignVariableInstruction;
import semulator.instructions.IncreaseInstruction;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import semulator.variable.Variable;

public class TestDivideSubset {
    public static void main(String[] args) {
        try {
            // Load the divide program to get access to its functions
            SProgram fullProgram = new SProgramImpl("Divide");
            Path xmlPath = Path.of(System.getProperty("user.dir") + "/test_divide.xml");
            fullProgram.validate(xmlPath);
            fullProgram.load();

            // Create a simplified program with just the counter logic
            SProgram program = new SProgramImpl("DivideSubset");

            // Copy functions from the full program
            if (program instanceof SProgramImpl programImpl && fullProgram instanceof SProgramImpl fullImpl) {
                // We need to access the functions somehow
            }

            // Simple program: increment z4 twice, then assign to y
            program.addInstruction(new IncreaseInstruction(Variable.of("z4")));
            program.addInstruction(new IncreaseInstruction(Variable.of("z4")));
            program.addInstruction(new AssignVariableInstruction(Variable.RESULT, Variable.of("z4")));

            System.out.println("=== Divide Subset Test ===");

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
