import semulator.program.SProgramImpl;

public class TestDivideDegree {
    public static void main(String[] args) {
        try {
            System.out.println("Testing degree calculation for divide program...");
            
            SProgramImpl program = new SProgramImpl("Divide");
            String result = program.validate(java.nio.file.Paths.get("test_divide.xml").toAbsolutePath());
            
            if (!"Valid".equals(result)) {
                System.err.println("Failed to validate XML: " + result);
                return;
            }
            
            program.load();
            
            int degree = program.calculateMaxDegree();
            System.out.println("Maximum degree: " + degree);
            System.out.println("Expected degree: 9");
            
            if (degree == 9) {
                System.out.println("✓ Test passed!");
            } else {
                System.out.println("✗ Test failed - expected 9, got " + degree);
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
