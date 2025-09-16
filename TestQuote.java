import semulator.program.SProgramImpl;
import java.nio.file.Path;

public class TestQuote {
    public static void main(String[] args) {
        System.out.println("Starting TestQuote...");
        try {
            SProgramImpl program = new SProgramImpl("TestQuote");
            Path xmlPath = Path.of("C:\\Users\\user\\IdeaProjects\\S-emulator\\test_quote.xml");

            System.out.println("Validating XML file...");
            String validation = program.validate(xmlPath);
            System.out.println("Validation result: " + validation);

            if ("Valid".equals(validation)) {
                System.out.println("Loading XML file...");
                Object result = program.load();
                System.out.println("Load result: " + result);

                System.out.println("Number of instructions: " + program.getInstructions().size());
                System.out.println("Number of functions: " + program.getFunctions().size());

                for (String funcName : program.getFunctions().keySet()) {
                    System.out.println("Function " + funcName + " has " +
                            program.getFunctions().get(funcName).size() + " instructions");
                }

                System.out.println("Testing expansion...");
                var expansion = program.expandToDegree(1);
                System.out.println("Expansion successful! Final program has " +
                        expansion.instructions().size() + " instructions");

            } else {
                System.out.println("Validation failed: " + validation);
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
