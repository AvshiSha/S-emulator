package ui;

import semulator.instructions.*;
import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.label.LabelImpl;
import semulator.variable.Variable;
import semulator.variable.VariableImpl;
import semulator.variable.VariableType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MockGateway implements ProgramGateway {

    private List<SInstruction> prog = new ArrayList<>();
    private final List<RunResult> hist = new ArrayList<>();
    private int runNo = 0;

    @Override
    public LoadResult load(Path xmlPath) {
        // דמו: נבנה תכנית קטנה:
        // L1: y <- y + 1
        //     IF x1 != 0 GOTO L1
        Variable y  = Variable.RESULT;
        Variable x1 = new VariableImpl(VariableType.INPUT, 1);
        Label L1 = new LabelImpl(1, 0);

        prog = List.of(
                new IncreaseInstruction(y, FixedLabel.EMPTY),
                new JumpNotZeroInstruction(x1, FixedLabel.EMPTY, L1)
        );
        return new LoadResult(true, "Loaded demo program from: " + xmlPath);
    }

    @Override public String show()   { return pretty(prog); }
    @Override public String expand(int level) { return pretty(prog); }

    @Override
    public RunResult run(int level, String inputsCsv) {
        // דמו בלבד: לא מריץ אמיתי
        long y = 1;
        long cycles = 1 + 2; // INC + IFNZ
        var r = new RunResult(++runNo, level, inputsCsv, y, cycles);
        hist.add(r);
        return r;
    }

    @Override public List<RunResult> history() { return List.copyOf(hist); }

    private static String pretty(List<SInstruction> instrs) {
        StringBuilder sb = new StringBuilder();
        // כותרות קצרות — בתרגיל האמיתי: שם התכנית, inputs בשימוש, labels בשימוש...
        for (int i = 0; i < instrs.size(); i++) {
            SInstruction in = instrs.get(i);
            String kind = (in instanceof JumpNotZeroInstruction
                    || in instanceof IncreaseInstruction
                    || in instanceof DecreaseInstruction
                    || in instanceof NoOpInstruction)
                    ? InstructionData.INCREASE.kindLetter() // נשתמש ב-"B"
                    : InstructionData.ZERO_VARIABLE.kindLetter(); // "S"

            // label 5-תווים:
            String labelBox = "[     ]";
            if (in.getLabel() != null && in.getLabel() != FixedLabel.EMPTY) {
                labelBox = String.format("[%-5s]", in.getLabel().getLabelRepresentation());
            }

            // טקסט הפקודה:
            String repr;
            if (in instanceof IncreaseInstruction) {
                repr = in.getVariable() + " -> " + in.getVariable() + " + 1";
            } else if (in instanceof DecreaseInstruction) {
                repr = in.getVariable() + " -> " + in.getVariable() + " - 1";
            } else if (in instanceof NoOpInstruction) {
                repr = in.getVariable() + " -> " + in.getVariable();
            } else if (in instanceof JumpNotZeroInstruction) {
                repr = "IF " + in.getVariable() + " != 0 GOTO L1"; // דמו
            } else {
                repr = in.getName(); // ברירת מחדל
            }

            sb.append(String.format("#%-3d (%s) %s %s (%d)%n",
                    (i + 1), kind, labelBox, repr, in.cycles()));
        }
        return sb.toString();
    }
}
