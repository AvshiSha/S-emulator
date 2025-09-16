import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import java.nio.file.Path;

public class DebugTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== DEBUG TEST ===");

            SProgram program = new SProgramImpl("test");
            Path xmlFile = Path.of("test_max_degree.xml").toAbsolutePath();

            String validation = program.validate(xmlFile);
            if (!"Valid".equals(validation)) {
                System.out.println("Validation failed: " + validation);
                return;
            }

            program.load();
            System.out.println("Program loaded successfully");

            // Print debug info
            System.out.println("Instructions in main program:");
            for (int i = 0; i < program.getInstructions().size(); i++) {
                System.out.println("  " + i + ": " + program.getInstructions().get(i).getName());
            }

            if (program instanceof SProgramImpl) {
                SProgramImpl impl = (SProgramImpl) program;
                System.out.println("Functions:");
                for (String funcName : impl.getFunctions().keySet()) {
                    System.out.println("  Function: " + funcName);
                    var funcInstructions = impl.getFunctions().get(funcName);
                    for (int i = 0; i < funcInstructions.size(); i++) {
                        System.out.println("    " + i + ": " + funcInstructions.get(i).getName());
                    }
                }
            }

            System.out.println("Calculating max degree...");
            int maxDegree = program.calculateMaxDegree();
            System.out.println("Final max degree: " + maxDegree);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
