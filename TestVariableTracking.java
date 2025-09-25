import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.program.ExpansionResult;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import semulator.instructions.SInstruction;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import semulator.variable.Variable;

public class TestVariableTracking {
    public static void main(String[] args) {
        try {
            SProgram program = new SProgramImpl("Divide");
            Path xmlPath = Path.of(System.getProperty("user.dir") + "/test_divide.xml");
            program.validate(xmlPath);
            program.load();

            System.out.println("=== Variable Tracking Analysis ===");
            
            // Show original program instructions
            System.out.println("\n--- Original Program Instructions ---");
            for (int i = 0; i < program.getInstructions().size(); i++) {
                var inst = program.getInstructions().get(i);
                System.out.println((i+1) + ": " + inst.getName() + " -> " + inst.getVariable());
            }

            // Test degree 0
            System.out.println("\n--- Degree 0 Execution ---");
            ProgramExecutor executor0 = new ProgramExecutorImpl(program);
            long result0 = executor0.run(4L, 2L);
            System.out.println("Result: " + result0);
            Map<Variable, Long> state0 = executor0.variableState();
            
            System.out.println("Original program variables:");
            state0.keySet().forEach(v -> System.out.println("  " + v + " = " + state0.get(v)));

            // Test degree 1
            System.out.println("\n--- Degree 1 Expansion Analysis ---");
            ExpansionResult expansion1 = program.expandToDegree(1);
            List<SInstruction> instructions = expansion1.instructions();
            
            System.out.println("Total instructions: " + instructions.size());
            
            // Look for instructions that involve y, z3, z4
            System.out.println("\nInstructions involving y, z3, z4:");
            for (int i = 0; i < instructions.size(); i++) {
                var inst = instructions.get(i);
                String varName = inst.getVariable().toString();
                if (varName.contains("y") || varName.contains("z3") || varName.contains("z4")) {
                    System.out.println((i+1) + ": " + inst.getName() + " -> " + inst.getVariable());
                }
            }
            
            // Look for ASSIGNMENT instructions
            System.out.println("\nASSIGNMENT instructions:");
            for (int i = 0; i < instructions.size(); i++) {
                var inst = instructions.get(i);
                if (inst.getName().equals("ASSIGNMENT") || inst.getName().equals("ASSIGN")) {
                    System.out.println((i+1) + ": " + inst.getName() + " -> " + inst.getVariable());
                }
            }

            // Test degree 1 execution
            System.out.println("\n--- Degree 1 Execution ---");
            SProgram expandedProgram1 = new SProgramImpl("expanded1");
            for (var instruction : expansion1.instructions()) {
                expandedProgram1.addInstruction(instruction);
            }

            ProgramExecutor executor1 = new ProgramExecutorImpl(expandedProgram1);
            long result1 = executor1.run(4L, 2L);
            System.out.println("Result: " + result1);
            Map<Variable, Long> state1 = executor1.variableState();
            
            System.out.println("Expanded program variables:");
            state1.keySet().forEach(v -> System.out.println("  " + v + " = " + state1.get(v)));

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
