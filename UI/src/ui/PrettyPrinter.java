// ui/PrettyPrinter.java
package ui;

import semulator.instructions.*;
import semulator.label.FixedLabel;
import semulator.label.Label;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class PrettyPrinter {
    private PrettyPrinter() {}

    public static String show(TempProgramFactory.TempProgram p) {
        StringBuilder sb = new StringBuilder();

        // כותרות
        sb.append("Program: ").append(p.getName()).append("\n");
        sb.append("Inputs: ").append(formatInputs(p.getInputsUsed())).append("\n");
        sb.append("Labels: ").append(formatLabels(p.getLabelsUsed())).append("\n\n");

        // הוראות
        List<SInstruction> ins = p.getInstructions();
        for (int i = 0; i < ins.size(); i++) {
            SInstruction in = ins.get(i);
            String kind = kindLetter(in);                 // "B" או "S"
            String labelBox = labelBox(in.getLabel());    // "[L1  ]" או "[     ]"
            String text = renderInstruction(in);          // "x1 -> x1 + 1" וכו'
            int cycles = in.cycles();

            sb.append(String.format("#%-3d (%s) %s %s (%d)%n",
                    i + 1, kind, labelBox, text, cycles));
        }
        return sb.toString();
    }

    private static String formatInputs(Set<String> inputs) {
        return inputs.stream()
                .sorted(Comparator.comparingInt(PrettyPrinter::xIndex))
                .collect(Collectors.joining(", "));
    }
    private static int xIndex(String x) {
        try { return Integer.parseInt(x.substring(1)); }
        catch (Exception e) { return Integer.MAX_VALUE; }
    }

    private static String formatLabels(List<semulator.label.Label> labels) {
        return labels.stream()
                .sorted((a, b) -> {
                    boolean ea = a.isExit(), eb = b.isExit();
                    if (ea && !eb) return 1;   // EXIT תמיד בסוף
                    if (!ea && eb) return -1;
                    // מיון אלפביתי לפי הייצוג ("L1","L2","EXIT"...)
                    return a.getLabelRepresentation().compareTo(b.getLabelRepresentation());
                })
                .map(semulator.label.Label::getLabelRepresentation)
                .collect(java.util.stream.Collectors.joining(", "));
    }


    private static String labelBox(semulator.label.Label l) {
        if (l == null) return "[     ]";
        return l.getLabelRepresentation(); // רוחב 5
    }

    private static String kindLetter(SInstruction in) {
        if (in instanceof IncreaseInstruction
                || in instanceof DecreaseInstruction
                || in instanceof NoOpInstruction
                || in instanceof JumpNotZeroInstruction) {
            return "B";
        }
        return "S";
    }

    private static String renderInstruction(SInstruction in) {
        if (in instanceof IncreaseInstruction) {
            return in.getVariable() + " <- " + in.getVariable() + " + 1";
        } else if (in instanceof DecreaseInstruction) {
            return in.getVariable() + " <- " + in.getVariable() + " - 1";
        } else if (in instanceof NoOpInstruction) {
            return in.getVariable() + " <- " + in.getVariable();
        } else if (in instanceof ZeroVariableInstruction) {
            return in.getVariable() + " <- 0";
        } else if (in instanceof AssignVariableInstruction a) {
            return in.getVariable() + " <- " + a.getSource();
        } else if (in instanceof AssignConstantInstruction c) {
            return in.getVariable() + " <- " + c.getConstant();
        } else if (in instanceof GotoLabelInstruction g) {
            return "GOTO " + g.getTarget();
        } else if (in instanceof JumpNotZeroInstruction j) {
            return "IF " + j.getVariable() + " != 0 GOTO " + j.getTarget();
        } else if (in instanceof JumpZeroInstruction j) {
            return "IF " + j.getVariable() + " == 0 GOTO " + j.getTarget();
        } else if (in instanceof JumpEqualConstantInstruction j) {
            return "IF " + j.getVariable() + " == " + j.getConstant() + " GOTO " + j.getTarget();
        } else if (in instanceof JumpEqualVariableInstruction j) {
            return "IF " + j.getVariable() + " == " + j.getOther() + " GOTO " + j.getTarget();
        }
        return in.getName();
    }
}
