import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.program.ExpansionResult;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import semulator.variable.Variable;

public class TestVariableMapping {
    public static void main(String[] args) {
        try {
            SProgram program = new SProgramImpl("Divide");
            Path xmlPath = Path.of(System.getProperty("user.dir") + "/test_divide.xml");
            program.validate(xmlPath);
            program.load();

            System.out.println("=== Variable Mapping Analysis ===");

            // Test degree 0
            System.out.println("\n--- Degree 0 Variables ---");
            ProgramExecutor executor0 = new ProgramExecutorImpl(program);
            long result0 = executor0.run(4L, 2L);
            System.out.println("Result: " + result0);
            Map<Variable, Long> state0 = executor0.variableState();

            Set<String> originalVars = new TreeSet<>();
            state0.keySet().forEach(v -> originalVars.add(v.toString()));
            System.out.println("Original program variables:");
            originalVars.forEach(v -> System.out.println("  " + v + " = " + state0.get(Variable.of(v))));

            // Test degree 1
            System.out.println("\n--- Degree 1 Variables ---");
            ExpansionResult expansion1 = program.expandToDegree(1);
            SProgram expandedProgram1 = new SProgramImpl("expanded1");
            for (var instruction : expansion1.instructions()) {
                expandedProgram1.addInstruction(instruction);
            }

            ProgramExecutor executor1 = new ProgramExecutorImpl(expandedProgram1);
            long result1 = executor1.run(4L, 2L);
            System.out.println("Result: " + result1);
            Map<Variable, Long> state1 = executor1.variableState();

            Set<String> expandedVars = new TreeSet<>();
            state1.keySet().forEach(v -> expandedVars.add(v.toString()));
            System.out.println("Expanded program variables:");
            expandedVars.forEach(v -> System.out.println("  " + v + " = " + state1.get(Variable.of(v))));

            // Check if original variables exist in expanded program
            System.out.println("\n--- Variable Existence Check ---");
            for (String varName : originalVars) {
                Variable var = Variable.of(varName);
                if (state1.containsKey(var)) {
                    System.out.println("  " + varName + ": EXISTS = " + state1.get(var));
                } else {
                    System.out.println("  " + varName + ": MISSING");
                }
            }

            // Look for variables with high values (potential results)
            System.out.println("\n--- High Value Variables ---");
            state1.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                    .forEach(entry -> System.out.println("  " + entry.getKey() + " = " + entry.getValue()));

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
