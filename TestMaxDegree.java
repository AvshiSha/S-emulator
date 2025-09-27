import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import java.nio.file.Path;

public class TestMaxDegree {
    public static void main(String[] args) {
        try {
            // Load the test XML file
            SProgram program = new SProgramImpl("f(x)=x+3");
            Path xmlFile = Path.of("test_max_degree.xml").toAbsolutePath();

            System.out.println("Loading XML file: " + xmlFile);

            // Validate first
            String validation = program.validate(xmlFile);
            if (!"Valid".equals(validation)) {
                System.out.println("Validation failed: " + validation);
                return;
            }

            Object result = program.load();

            if (result == null) {
                System.out.println("Failed to load XML file");
                return;
            }

            System.out.println("XML file loaded successfully");
            System.out.println("Program name: " + program.getName());
            System.out.println("Number of instructions: " + program.getInstructions().size());

            // Calculate maximum degree
            System.out.println("=== CALCULATING MAXIMUM DEGREE ===");
            int maxDegree = program.calculateMaxDegree();
            System.out.println("=== END CALCULATION ===");
            System.out.println("Maximum degree: " + maxDegree);

            // Expected: JUMP_EQUAL_FUNCTION calls Const (degree 2) + 1 = 3
            // QUOTE calls Const (degree 2) + 1 = 3
            // ASSIGNMENT = degree 2
            // So maximum should be 3

            System.out.println("Expected maximum degree: 3");
            System.out.println("Actual maximum degree: " + maxDegree);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
