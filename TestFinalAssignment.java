import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.program.ExpansionResult;
import semulator.instructions.SInstruction;
import semulator.instructions.AssignVariableInstruction;
import java.nio.file.Path;
import java.util.List;

public class TestFinalAssignment {
    public static void main(String[] args) {
        try {
            SProgram program = new SProgramImpl("Divide");
            Path xmlPath = Path.of(System.getProperty("user.dir") + "/test_divide.xml");
            program.validate(xmlPath);
            program.load();

            System.out.println("=== Final Assignment Analysis ===");
            
            // Show original program instructions
            System.out.println("\n--- Original Program Instructions ---");
            for (int i = 0; i < program.getInstructions().size(); i++) {
                var inst = program.getInstructions().get(i);
                System.out.println((i+1) + ": " + inst.getName() + " -> " + inst.getVariable());
                if (inst.getName().equals("ASSIGNMENT") || inst.getName().equals("ASSIGN")) {
                    if (inst instanceof AssignVariableInstruction assign) {
                        System.out.println("    Source: " + assign.getSource());
                    }
                }
            }

            // Test degree 1
            System.out.println("\n--- Degree 1 Expansion Analysis ---");
            ExpansionResult expansion1 = program.expandToDegree(1);
            List<SInstruction> instructions = expansion1.instructions();
            
            System.out.println("Total instructions: " + instructions.size());
            
            // Look for the last few instructions
            System.out.println("\nLast 10 instructions:");
            int start = Math.max(0, instructions.size() - 10);
            for (int i = start; i < instructions.size(); i++) {
                var inst = instructions.get(i);
                System.out.println((i+1) + ": " + inst.getName() + " -> " + inst.getVariable());
                if (inst.getName().equals("ASSIGNMENT") || inst.getName().equals("ASSIGN")) {
                    if (inst instanceof AssignVariableInstruction assign) {
                        System.out.println("    Source: " + assign.getSource());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
