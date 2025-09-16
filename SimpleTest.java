import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import java.nio.file.Path;

public class SimpleTest {
    public static void main(String[] args) {
        try {
            System.out.println("Starting test...");

            SProgram program = new SProgramImpl("test");
            Path xmlFile = Path.of("test_max_degree.xml").toAbsolutePath();

            System.out.println("Validating...");
            String validation = program.validate(xmlFile);
            System.out.println("Validation result: " + validation);

            if (!"Valid".equals(validation)) {
                return;
            }

            System.out.println("Loading...");
            Object result = program.load();
            System.out.println("Load result: " + result);

            System.out.println("Calculating max degree...");
            int maxDegree = program.calculateMaxDegree();
            System.out.println("Max degree: " + maxDegree);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
