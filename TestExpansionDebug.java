import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.program.ExpansionResult;
import java.nio.file.Path;

public class TestExpansionDebug {
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
            System.out.println("Max degree: " + program.calculateMaxDegree());

            // Test expansion to see what instructions remain
            for (int degree = 0; degree <= 3; degree++) {
                System.out.println("\n=== Degree " + degree + " ===");
                ExpansionResult expansion = program.expandToDegree(degree);
                System.out.println("Expanded instructions count: " + expansion.instructions().size());
                
                // Count QUOTE instructions
                long quoteCount = expansion.instructions().stream()
                    .filter(inst -> inst.getName().equals("QUOTE"))
                    .count();
                System.out.println("QUOTE instructions count: " + quoteCount);
                
                // Show first few instructions
                System.out.println("First 10 instructions:");
                for (int i = 0; i < Math.min(10, expansion.instructions().size()); i++) {
                    var inst = expansion.instructions().get(i);
                    System.out.println("  " + (i+1) + ": " + inst.getName() + " -> " + inst.getVariable());
                }
                
                // Show any remaining QUOTE instructions
                if (quoteCount > 0) {
                    System.out.println("Remaining QUOTE instructions:");
                    for (int i = 0; i < expansion.instructions().size(); i++) {
                        var inst = expansion.instructions().get(i);
                        if (inst.getName().equals("QUOTE")) {
                            System.out.println("  " + (i+1) + ": " + inst);
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
