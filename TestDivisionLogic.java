import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import java.io.File;
import java.nio.file.Path;

public class TestDivisionLogic {
    public static void main(String[] args) {
        try {
            // Create and load the division program
            SProgramImpl program = new SProgramImpl("Divide");
            Path xmlPath = Path.of("test_divide.xml").toAbsolutePath();
            program.validate(xmlPath);
            program.load();

            ProgramExecutor executor = new ProgramExecutorImpl(program);

            System.out.println("=== Division Algorithm Analysis ===");
            System.out.println("Input: x1=2, x2=1");
            System.out.println("Expected: y=2 (since 2 ÷ 1 = 2)");
            System.out.println();

            // Execute the program
            long result = executor.run(2L, 1L);
            System.out.println("Actual result: y=" + result);

            // Get the execution context to see intermediate values
            if (executor instanceof ProgramExecutorImpl) {
                ProgramExecutorImpl impl = (ProgramExecutorImpl) executor;
                System.out.println("\n=== Execution Context ===");
                System.out.println("Total cycles: " + impl.getTotalCycles());
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
