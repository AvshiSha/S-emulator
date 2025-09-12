import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;

public class TestCycleCounting {
    public static void main(String[] args) {
        try {
            // Load the test program
            SProgram program = SProgramImpl.fromXml("test_simple_cycles.xml");

            // Run the program
            ProgramExecutor executor = new ProgramExecutorImpl(program);
            long result = executor.run(5L, 5L); // x1=5, x2=5

            System.out.println("Result: " + result);
            System.out.println("Total cycles: " + executor.getTotalCycles());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
