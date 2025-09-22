import semulator.program.SProgramImpl;
import semulator.program.ExpansionResult;
import semulator.instructions.SInstruction;
import semulator.instructions.QuoteInstruction;
import java.nio.file.Path;

public class TestComposition {
    public static void main(String[] args) {
        System.out.println("Testing Function Composition Fix...");
        try {
            // Create a program with composition
            SProgramImpl program = new SProgramImpl("TestComposition");
            
            // Add the AND function
            program.getFunctions().put("AND", createAndFunction());
            
            // Add the Smaller_Equal_Than function (simplified)
            program.getFunctions().put("Smaller_Equal_Than", createSmallerEqualThanFunction());
            
            // Add the NOT function
            program.getFunctions().put("NOT", createNotFunction());
            
            // Add the EQUAL function
            program.getFunctions().put("EQUAL", createEqualFunction());
            
            // Add the main instruction with composition
            // z6 <- (AND,(Smaller_Equal_Than,x1,x2),(NOT,(EQUAL,x2,x1)))
            var quoteInstruction = createCompositionInstruction();
            program.addInstruction(quoteInstruction);
            
            System.out.println("Program created with " + program.getInstructions().size() + " instructions");
            System.out.println("Functions: " + program.getFunctions().keySet());
            
            // Test expansion to degree 1
            System.out.println("\n=== Expanding to degree 1 ===");
            ExpansionResult expansion = program.expandToDegree(1);
            
            System.out.println("Expanded program has " + expansion.instructions().size() + " instructions:");
            for (int i = 0; i < expansion.instructions().size(); i++) {
                SInstruction inst = expansion.instructions().get(i);
                System.out.println((i+1) + ": " + inst);
                
                // Check if we have the expected QUOTE instructions
                if (inst instanceof QuoteInstruction) {
                    QuoteInstruction quote = (QuoteInstruction) inst;
                    System.out.println("    -> QUOTE: " + quote.getVariable() + " <- (" + 
                                     quote.getFunctionName() + ", " + quote.getFunctionArguments() + ")");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static java.util.List<SInstruction> createAndFunction() {
        // AND function: y <- 0; IF x1 == 0 GOTO EXIT; IF x2 == 0 GOTO EXIT; y <- y + 1
        var instructions = new java.util.ArrayList<SInstruction>();
        
        // y <- 0
        instructions.add(new semulator.instructions.ZeroVariableInstruction(semulator.variable.Variable.RESULT));
        
        // IF x1 == 0 GOTO EXIT
        instructions.add(new semulator.instructions.JumpZeroInstruction(
            new semulator.variable.VariableImpl(semulator.variable.VariableType.INPUT, 1),
            semulator.label.FixedLabel.EXIT));
        
        // IF x2 == 0 GOTO EXIT  
        instructions.add(new semulator.instructions.JumpZeroInstruction(
            new semulator.variable.VariableImpl(semulator.variable.VariableType.INPUT, 2),
            semulator.label.FixedLabel.EXIT));
        
        // y <- y + 1
        instructions.add(new semulator.instructions.IncreaseInstruction(semulator.variable.Variable.RESULT));
        
        return instructions;
    }
    
    private static java.util.List<SInstruction> createSmallerEqualThanFunction() {
        // Simplified Smaller_Equal_Than function
        var instructions = new java.util.ArrayList<SInstruction>();
        
        // y <- 1 (assume x1 <= x2)
        instructions.add(new semulator.instructions.ZeroVariableInstruction(semulator.variable.Variable.RESULT));
        instructions.add(new semulator.instructions.IncreaseInstruction(semulator.variable.Variable.RESULT));
        
        return instructions;
    }
    
    private static java.util.List<SInstruction> createNotFunction() {
        // NOT function: y <- 1 - x1
        var instructions = new java.util.ArrayList<SInstruction>();
        
        // y <- 1
        instructions.add(new semulator.instructions.ZeroVariableInstruction(semulator.variable.Variable.RESULT));
        instructions.add(new semulator.instructions.IncreaseInstruction(semulator.variable.Variable.RESULT));
        
        // y <- y - x1
        var x1 = new semulator.variable.VariableImpl(semulator.variable.VariableType.INPUT, 1);
        var temp = new semulator.variable.VariableImpl(semulator.variable.VariableType.WORK, 1);
        
        // temp <- x1
        instructions.add(new semulator.instructions.AssignVariableInstruction(temp, x1));
        
        // while temp != 0: y <- y - 1, temp <- temp - 1
        var loopLabel = new semulator.label.LabelImpl("LOOP");
        var exitLabel = semulator.label.FixedLabel.EXIT;
        
        instructions.add(new semulator.instructions.JumpZeroInstruction(temp, exitLabel, loopLabel));
        instructions.add(new semulator.instructions.DecreaseInstruction(semulator.variable.Variable.RESULT, loopLabel));
        instructions.add(new semulator.instructions.DecreaseInstruction(temp, loopLabel));
        instructions.add(new semulator.instructions.JumpNotZeroInstruction(temp, loopLabel));
        
        return instructions;
    }
    
    private static java.util.List<SInstruction> createEqualFunction() {
        // EQUAL function: y <- 1 if x1 == x2, else 0
        var instructions = new java.util.ArrayList<SInstruction>();
        
        // y <- 0
        instructions.add(new semulator.instructions.ZeroVariableInstruction(semulator.variable.Variable.RESULT));
        
        // temp1 <- x1, temp2 <- x2
        var x1 = new semulator.variable.VariableImpl(semulator.variable.VariableType.INPUT, 1);
        var x2 = new semulator.variable.VariableImpl(semulator.variable.VariableType.INPUT, 2);
        var temp1 = new semulator.variable.VariableImpl(semulator.variable.VariableType.WORK, 1);
        var temp2 = new semulator.variable.VariableImpl(semulator.variable.VariableType.WORK, 2);
        
        instructions.add(new semulator.instructions.AssignVariableInstruction(temp1, x1));
        instructions.add(new semulator.instructions.AssignVariableInstruction(temp2, x2));
        
        // while temp1 != 0 and temp2 != 0: temp1--, temp2--
        var loopLabel = new semulator.label.LabelImpl("LOOP");
        var exitLabel = semulator.label.FixedLabel.EXIT;
        
        instructions.add(new semulator.instructions.JumpZeroInstruction(temp1, exitLabel, loopLabel));
        instructions.add(new semulator.instructions.JumpZeroInstruction(temp2, exitLabel, loopLabel));
        instructions.add(new semulator.instructions.DecreaseInstruction(temp1, loopLabel));
        instructions.add(new semulator.instructions.DecreaseInstruction(temp2, loopLabel));
        instructions.add(new semulator.instructions.JumpNotZeroInstruction(temp1, loopLabel));
        
        // if temp1 == 0 and temp2 == 0: y <- 1
        instructions.add(new semulator.instructions.JumpZeroInstruction(temp2, exitLabel));
        instructions.add(new semulator.instructions.IncreaseInstruction(semulator.variable.Variable.RESULT));
        
        return instructions;
    }
    
    private static QuoteInstruction createCompositionInstruction() {
        // z6 <- (AND,(Smaller_Equal_Than,x1,x2),(NOT,(EQUAL,x2,x1)))
        var z6 = new semulator.variable.VariableImpl(semulator.variable.VariableType.WORK, 6);
        
        // Create the function arguments
        var x1 = new semulator.variable.VariableImpl(semulator.variable.VariableType.INPUT, 1);
        var x2 = new semulator.variable.VariableImpl(semulator.variable.VariableType.INPUT, 2);
        
        // (Smaller_Equal_Than,x1,x2)
        var smallerEqualThanCall = new semulator.instructions.FunctionCallArgument("Smaller_Equal_Than", 
            java.util.List.of(new semulator.instructions.VariableArgument(x1), 
                            new semulator.instructions.VariableArgument(x2)));
        
        // (EQUAL,x2,x1)
        var equalCall = new semulator.instructions.FunctionCallArgument("EQUAL", 
            java.util.List.of(new semulator.instructions.VariableArgument(x2), 
                            new semulator.instructions.VariableArgument(x1)));
        
        // (NOT,(EQUAL,x2,x1))
        var notCall = new semulator.instructions.FunctionCallArgument("NOT", 
            java.util.List.of(equalCall));
        
        // (AND,(Smaller_Equal_Than,x1,x2),(NOT,(EQUAL,x2,x1)))
        var andCall = new semulator.instructions.FunctionCallArgument("AND", 
            java.util.List.of(smallerEqualThanCall, notCall));
        
        return new QuoteInstruction(z6, "AND", 
            java.util.List.of(smallerEqualThanCall, notCall), 
            createAndFunction());
    }
}
