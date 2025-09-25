import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.program.ExpansionResult;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import java.nio.file.Path;

public class TestDebugExpansion {
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

            System.out.println("=== Divide program loaded successfully! ===");
            System.out.println("Program name: " + program.getName());
            System.out.println("Number of instructions: " + program.getInstructions().size());

            // Print the original instructions to understand the structure
            System.out.println("\n=== Original Program Instructions ===");
            for (int i = 0; i < program.getInstructions().size(); i++) {
                var inst = program.getInstructions().get(i);
                System.out.println((i + 1) + ": " + inst.getName() + " -> " + inst.getVariable());
                if (inst.getName().equals("QUOTE")) {
                    System.out.println(
                            "    Function: " + inst.getClass().getDeclaredMethod("getFunctionName").invoke(inst));
                    System.out.println(
                            "    Arguments: " + inst.getClass().getDeclaredMethod("getFunctionArguments").invoke(inst));
                }
            }

            // Test expansion to degree 1
            System.out.println("\n=== Expansion to Degree 1 ===");
            ExpansionResult expansion1 = program.expandToDegree(1);
            System.out.println("Expanded instructions count: " + expansion1.instructions().size());

            // Show first 20 instructions to see what the expansion looks like
            System.out.println("\nFirst 20 expanded instructions:");
            for (int i = 0; i < Math.min(20, expansion1.instructions().size()); i++) {
                var inst = expansion1.instructions().get(i);
                System.out.println((i + 1) + ": " + inst.getName() + " -> " + inst.getVariable());
            }

            // Test execution with a simple case: x1=4, x2=2
            System.out.println("\n=== Testing Execution ===");
            System.out.println("Input: x1=4, x2=2, Expected: 2");

            // Original program
            ProgramExecutor executor0 = new ProgramExecutorImpl(program);
            long result0 = executor0.run(4L, 2L);
            System.out.println("Original program result: " + result0 + " (Expected: 2)");

            // Expanded program
            SProgram expandedProgram1 = new SProgramImpl("expanded1");
            for (var instruction : expansion1.instructions()) {
                expandedProgram1.addInstruction(instruction);
            }
            if (expandedProgram1 instanceof SProgramImpl programImpl) {
                programImpl.copyFunctionsFrom(program);
            }

            ProgramExecutor executor1 = new ProgramExecutorImpl(expandedProgram1);
            long result1 = executor1.run(4L, 2L);
            System.out.println("Expanded program result: " + result1 + " (Expected: 2)");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
