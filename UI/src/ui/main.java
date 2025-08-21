package ui;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import semulator.execution.ProgramExecutor;
import semulator.execution.ProgramExecutorImpl;
import semulator.instructions.*;
import semulator.label.LabelImpl;
import semulator.parsing.ProgramParsing;
import semulator.program.SProgram;
import semulator.program.SProgramImpl;
import semulator.variable.Variable;
import semulator.variable.VariableImpl;
import semulator.variable.VariableType;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class main {
    public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException {
//        Variable x1 = new VariableImpl(VariableType.INPUT, 1);
//        Variable z1 = new VariableImpl(VariableType.WORK, 1);
//
//        LabelImpl l1 = new LabelImpl(1);
//        LabelImpl l2 = new LabelImpl(1);
//
//        SInstruction increase = new IncreaseInstruction(x1, l1);
//        SInstruction decrease = new DecreaseInstruction(z1, l2);
//        SInstruction noop = new NoOpInstruction(Variable.RESULT);
//        SInstruction jnz = new JumpNotZeroInstruction(x1, l2);
//
//        SProgram p = new SProgramImpl("test");
//        p.addInstruction(increase);
//        p.addInstruction(increase);
//        p.addInstruction(decrease);
//        p.addInstruction(jnz);
//
//        ProgramExecutor programExecutor = new ProgramExecutorImpl(p);
//        long result = programExecutor.run(3L, 6L, 2L);
//        System.out.println(result);
//        ;
//
//
//        sanity();
        ProgramParsing.PathCheckResult check = ProgramParsing.askPathAndCheckFromConsole();
        if (!check.ok()) {
            System.out.println("Path check failed:");
            for (String e : check.errors()) System.out.println("  - " + e);
            return;
        }

        // 2) Parse the XML into an in-memory program (no MDB commit here)
        Document parsed = ProgramParsing.parseXml(check.path());
        if (parsed == null) {
            System.out.println("Parse failed:");
            return;
        }

        System.out.println("Program is valid and ready to load.");

    }

    private static void sanity() {
        /*

        {y = x1}

        [L1] x1 ← x1 – 1
             y ← y + 1
             IF x1 != 0 GOTO L1
        * */

        Variable x1 = new VariableImpl(VariableType.INPUT, 1);
        LabelImpl l1 = new LabelImpl(1);

        SProgram p = new SProgramImpl("SANITY");
        p.addInstruction(new DecreaseInstruction(x1, l1));
        p.addInstruction(new IncreaseInstruction(Variable.RESULT));
        p.addInstruction(new JumpNotZeroInstruction(x1, l1));

        long result = new ProgramExecutorImpl(p).run(4L);
        System.out.println(result);
    }
}