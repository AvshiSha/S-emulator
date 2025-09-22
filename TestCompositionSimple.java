import semulator.program.SProgramImpl;
import semulator.program.ExpansionResult;
import semulator.instructions.SInstruction;
import semulator.instructions.QuoteInstruction;
import semulator.instructions.FunctionCallArgument;
import semulator.instructions.VariableArgument;
import semulator.variable.VariableImpl;
import semulator.variable.VariableType;
import java.util.List;

public class TestCompositionSimple {
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
                System.out.println((i + 1) + ": " + inst);

                // Check if we have the expected QUOTE instructions
                if (inst instanceof QuoteInstruction) {
                    QuoteInstruction quote = (QuoteInstruction) inst;
                    System.out.println("    -> QUOTE: " + quote.getVariable() + " <- (" +
                            quote.getFunctionName() + ", " + quote.getFunctionArguments() + ")");
                }
            }

            // Check if we have the expected pattern
            System.out.println("\n=== Checking for expected pattern ===");
            boolean foundSmallerEqualQuote = false;
            boolean foundNotQuote = false;

            for (SInstruction inst : expansion.instructions()) {
                if (inst instanceof QuoteInstruction) {
                    QuoteInstruction quote = (QuoteInstruction) inst;
                    String varName = quote.getVariable().toString();
                    String funcName = quote.getFunctionName();

                    if (funcName.equals("Smaller_Equal_Than")) {
                        foundSmallerEqualQuote = true;
                        System.out.println("✓ Found " + varName + " <- (Smaller_Equal_Than, x1, x2)");
                    }
                    if (funcName.equals("NOT")) {
                        foundNotQuote = true;
                        System.out.println("✓ Found " + varName + " <- (NOT, (EQUAL, x2, x1))");
                    }
                }
            }

            if (foundSmallerEqualQuote && foundNotQuote) {
                System.out.println("\n🎉 SUCCESS: Composition fix is working correctly!");
                System.out.println("The nested function calls are now properly created as QUOTE instructions");
                System.out.println("that will be expanded in the next degree, not fully expanded now.");
                System.out.println("\nThis matches your expected behavior:");
                System.out.println("z1 <- (Smaller_Equal_Than,x1,x2)  // z1 is new working var in P");
                System.out.println("z2 <- (NOT,(EQUAL,x2,x1))        // z2 is a new working var in P");
            } else {
                System.out.println("\n❌ ISSUE: Expected pattern not found");
                System.out.println("Found Smaller_Equal_Than quote: " + foundSmallerEqualQuote);
                System.out.println("Found NOT quote: " + foundNotQuote);
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<SInstruction> createAndFunction() {
        // AND function: y <- 0; IF x1 == 0 GOTO EXIT; IF x2 == 0 GOTO EXIT; y <- y + 1
        var instructions = new java.util.ArrayList<SInstruction>();

        // y <- 0
        instructions.add(new semulator.instructions.ZeroVariableInstruction(semulator.variable.Variable.RESULT));

        // IF x1 == 0 GOTO EXIT
        instructions.add(new semulator.instructions.JumpZeroInstruction(
                new VariableImpl(VariableType.INPUT, 1),
                semulator.label.FixedLabel.EXIT));

        // IF x2 == 0 GOTO EXIT
        instructions.add(new semulator.instructions.JumpZeroInstruction(
                new VariableImpl(VariableType.INPUT, 2),
                semulator.label.FixedLabel.EXIT));

        // y <- y + 1
        instructions.add(new semulator.instructions.IncreaseInstruction(semulator.variable.Variable.RESULT));

        return instructions;
    }

    private static List<SInstruction> createSmallerEqualThanFunction() {
        // Simplified Smaller_Equal_Than function
        var instructions = new java.util.ArrayList<SInstruction>();

        // y <- 1 (assume x1 <= x2)
        instructions.add(new semulator.instructions.ZeroVariableInstruction(semulator.variable.Variable.RESULT));
        instructions.add(new semulator.instructions.IncreaseInstruction(semulator.variable.Variable.RESULT));

        return instructions;
    }

    private static List<SInstruction> createNotFunction() {
        // NOT function: y <- 1 - x1
        var instructions = new java.util.ArrayList<SInstruction>();

        // y <- 1
        instructions.add(new semulator.instructions.ZeroVariableInstruction(semulator.variable.Variable.RESULT));
        instructions.add(new semulator.instructions.IncreaseInstruction(semulator.variable.Variable.RESULT));

        // y <- y - x1
        var x1 = new VariableImpl(VariableType.INPUT, 1);
        var temp = new VariableImpl(VariableType.WORK, 1);

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

    private static List<SInstruction> createEqualFunction() {
        // EQUAL function: y <- 1 if x1 == x2, else 0
        var instructions = new java.util.ArrayList<SInstruction>();

        // y <- 0
        instructions.add(new semulator.instructions.ZeroVariableInstruction(semulator.variable.Variable.RESULT));

        // temp1 <- x1, temp2 <- x2
        var x1 = new VariableImpl(VariableType.INPUT, 1);
        var x2 = new VariableImpl(VariableType.INPUT, 2);
        var temp1 = new VariableImpl(VariableType.WORK, 1);
        var temp2 = new VariableImpl(VariableType.WORK, 2);

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
        var z6 = new VariableImpl(VariableType.WORK, 6);

        // Create the function arguments
        var x1 = new VariableImpl(VariableType.INPUT, 1);
        var x2 = new VariableImpl(VariableType.INPUT, 2);

        // (Smaller_Equal_Than,x1,x2)
        var smallerEqualThanCall = new FunctionCallArgument("Smaller_Equal_Than",
                List.of(new VariableArgument(x1), new VariableArgument(x2)));

        // (EQUAL,x2,x1)
        var equalCall = new FunctionCallArgument("EQUAL",
                List.of(new VariableArgument(x2), new VariableArgument(x1)));

        // (NOT,(EQUAL,x2,x1))
        var notCall = new FunctionCallArgument("NOT", List.of(equalCall));

        // (AND,(Smaller_Equal_Than,x1,x2),(NOT,(EQUAL,x2,x1)))
        return new QuoteInstruction(z6, "AND",
                List.of(smallerEqualThanCall, notCall),
                createAndFunction());
    }
}
