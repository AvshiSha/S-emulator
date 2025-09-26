import semulator.program.SProgramImpl;
import semulator.program.ExpansionResult;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestFxExpansion {
    public static void main(String[] args) {
        try {
            // Create a program and load the test_fx_program.xml file
            SProgramImpl program = new SProgramImpl("TestFx");

            Path xmlPath = Paths.get("C:\\Users\\user\\IdeaProjects\\S-emulator\\test_fx_program.xml");
            String validation = program.validate(xmlPath);
            if (!"Valid".equals(validation)) {
                System.err.println("XML validation failed: " + validation);
                return;
            }

            // Load the XML file
            program.load();

            // Expand the main program to degree 1
            ExpansionResult result = program.expandToDegree(1);

            System.out.println("Main program expansion to degree 1:");
            System.out.println("Number of instructions: " + result.instructions().size());

            for (int i = 0; i < result.instructions().size(); i++) {
                System.out.println((i + 1) + ". " + result.instructions().get(i).toString());
            }

            System.out.println("\nExpected expansion should be:");
            System.out.println("1. z1 <- (Const7)");
            System.out.println("2. z2 <- (Successor,x1)");
            System.out.println("3. z3 <- z1 // z3 as the output variable");
            System.out.println("4. z4 <- z2");
            System.out.println("5. L3 IF z4 != 0 GOTO L1");
            System.out.println("6. GOTO L4");
            System.out.println("7. L1 IF z3 != 0 GOTO L2");
            System.out.println("8. GOTO L4");
            System.out.println("9. L2 z3 <- z3 - 1");
            System.out.println("10. z4 <- z4 - 1");
            System.out.println("11. GOTO L3");
            System.out.println("12. L4 y <- z3");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
