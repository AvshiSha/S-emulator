package ui;

import org.xml.sax.SAXException;
// import semulator.execution.ProgramExecutor;
//import semulator.execution.ProgramExecutorImpl;
//import semulator.instructions.*;
//import semulator.label.LabelImpl;
//import semulator.program.SProgram;
import semulator.program.SProgramImpl;
//import semulator.variable.Variable;
//import semulator.variable.VariableImpl;
//import semulator.variable.VariableType;

import javax.xml.parsers.ParserConfigurationException;
// import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {

        // Test our implementation first
        // testProgramExecutor();

        // Then start the console UI
        new ConsoleUI(new SProgramImpl("S")).start();
    }

    private static void testProgramExecutor() {
        System.out.println("=== Testing ProgramExecutor Implementation ===");

        // Test 1: Simple program that copies x1 to y
        System.out.println("\nTest 1: Copy x1 to y");
        semulator.variable.Variable x1 = new semulator.variable.VariableImpl(semulator.variable.VariableType.INPUT, 1);
        semulator.variable.Variable y = semulator.variable.Variable.RESULT;

        semulator.program.SProgram p1 = new semulator.program.SProgramImpl("copy");
        p1.addInstruction(new semulator.instructions.AssignVariableInstruction(y, x1));

        semulator.execution.ProgramExecutor executor1 = new semulator.execution.ProgramExecutorImpl(p1);
        long result1 = executor1.run(5L);
        System.out.printf("Input: 5, Result: %d, Cycles: %d%n", result1, executor1.getTotalCycles());

        // Test 2: Simple increment program
        System.out.println("\nTest 2: Simple increment program");
        semulator.program.SProgram p2 = new semulator.program.SProgramImpl("increment");
        p2.addInstruction(new semulator.instructions.IncreaseInstruction(y)); // y <- y + 1
        p2.addInstruction(new semulator.instructions.IncreaseInstruction(y)); // y <- y + 1
        p2.addInstruction(new semulator.instructions.IncreaseInstruction(y)); // y <- y + 1

        // Test 3: Simple loop test
        System.out.println("\nTest 3: Simple loop test");
        semulator.label.LabelImpl l1 = new semulator.label.LabelImpl(1, 0);
        semulator.program.SProgram p3 = new semulator.program.SProgramImpl("loop");
        p3.addInstruction(new semulator.instructions.ZeroVariableInstruction(y, l1)); // [L1] y <- 0
        p3.addInstruction(new semulator.instructions.IncreaseInstruction(y)); // y <- y + 1
        p3.addInstruction(new semulator.instructions.IncreaseInstruction(y)); // y <- y + 1
        p3.addInstruction(new semulator.instructions.JumpNotZeroInstruction(x1, l1)); // IF x1 != 0 GOTO L1

        semulator.execution.ProgramExecutor executor2 = new semulator.execution.ProgramExecutorImpl(p2);
        long result2 = executor2.run();
        System.out.printf("Result: %d, Cycles: %d%n", result2, executor2.getTotalCycles());

        semulator.execution.ProgramExecutor executor3 = new semulator.execution.ProgramExecutorImpl(p3);
        long result3 = executor3.run(2L); // x1 = 2
        System.out.printf("Loop test - Input: 2, Result: %d, Cycles: %d%n", result3, executor3.getTotalCycles());

        // Test 4: Check variable states
        System.out.println("\nTest 4: Variable states");
        Map<semulator.variable.Variable, Long> state = executor3.variableState();
        for (Map.Entry<semulator.variable.Variable, Long> entry : state.entrySet()) {
            System.out.printf("%s = %d%n", entry.getKey(), entry.getValue());
        }

        System.out.println("=== Testing Complete ===\n");
    }

    // private static void sanity() {
    // /*
    //
    // {y = x1}
    //
    // [L1] x1 ← x1 – 1
    // y ← y + 1
    // IF x1 != 0 GOTO L1
    // * */
    //
    // Variable x1 = new VariableImpl(VariableType.INPUT, 1);
    // LabelImpl l1 = new LabelImpl(1);
    //
    // SProgram p = new SProgramImpl("SANITY");
    // p.addInstruction(new DecreaseInstruction(x1, l1));
    // p.addInstruction(new IncreaseInstruction(Variable.RESULT));
    // p.addInstruction(new JumpNotZeroInstruction(x1, l1));
    //
    // long result = new ProgramExecutorImpl(p).run(4L);
    // System.out.println(result);
    // }
}