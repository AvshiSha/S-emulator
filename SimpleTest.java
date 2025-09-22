import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import java.nio.file.Path;

public class SimpleTest {
    public static void main(String[] args) {
        try {
            SProgram program = new SProgramImpl("SimpleTest");
            Path xmlFile = Path.of("test_quote.xml").toAbsolutePath();

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

            int maxDegree = program.calculateMaxDegree();
            System.out.println("Maximum degree: " + maxDegree);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}