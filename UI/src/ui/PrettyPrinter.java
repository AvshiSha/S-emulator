// ui/PrettyPrinter.java
package ui;

import semulator.instructions.*;
import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.program.SProgram;   // <-- חדש

import java.util.*;
import java.util.stream.Collectors;

public final class PrettyPrinter {
    private PrettyPrinter() {}

    // === חדש: תצוגה של תוכנית אמיתית (SProgram) ===
    public static String show(SProgram p) {
        StringBuilder sb = new StringBuilder();

        // כותרות
        sb.append("Program: ").append(p.getName()).append("\n");

        // הפקה דינמית של Inputs/Labels מתוך ההוראות הקיימות
        List<SInstruction> ins = p.getInstructions();
        Set<String> inputsUsed = deriveInputs(ins);
        List<Label> labelsUsed = deriveLabels(ins);

        sb.append("Inputs: ").append(formatInputs(inputsUsed)).append("\n");
        sb.append("Labels: ").append(formatLabels(labelsUsed)).append("\n\n");

        // הוראות (אותה תצוגה שכבר כתבת)
        for (int i = 0; i < ins.size(); i++) {
            SInstruction in = ins.get(i);
            String kind = kindLetter(in);
            String labelBox = labelBox(in.getLabel());
            String text = renderInstruction(in);
            int cycles = in.cycles();

            sb.append(String.format("#%-3d (%s) %s %s (%d)%n",
                    i + 1, kind, labelBox, text, cycles));
        }
        return sb.toString();
    }

    // ---------- עזר: חישוב Inputs מתוך ההוראות ----------
    private static Set<String> deriveInputs(List<SInstruction> ins) {
        Set<String> xs = new HashSet<>();
        for (SInstruction in : ins) {
            // משתנה מרכזי
            if (in.getVariable() != null) {
                String v = in.getVariable().toString();
                if (v.startsWith("x")) xs.add(v);
            }
            // מקורות נוספים לפי סוגים שונים:
            if (in instanceof AssignVariableInstruction a && a.getSource() != null) {
                String s = a.getSource().toString();
                if (s.startsWith("x")) xs.add(s);
            }
            if (in instanceof JumpEqualVariableInstruction j && j.getOther() != null) {
                String o = j.getOther().toString();
                if (o.startsWith("x")) xs.add(o);
            }
            // אפשר להרחיב אם יש עוד פקודות שקוראות מקלטים
        }
        return xs;
    }

    // ---------- עזר: חישוב Labels בשימוש ----------
    private static List<Label> deriveLabels(List<SInstruction> ins) {
        List<Label> labels = new ArrayList<>();
        for (SInstruction in : ins) {
            if (in.getLabel() != null) labels.add(in.getLabel()); // לייבל שמוגדר על ההוראה
            if (in instanceof GotoLabelInstruction g && g.getTarget() != null) {
                labels.add(g.getTarget());
            }
            if (in instanceof JumpNotZeroInstruction j && j.getTarget() != null) {
                labels.add(j.getTarget());
            }
            if (in instanceof JumpZeroInstruction j && j.getTarget() != null) {
                labels.add(j.getTarget());
            }
            if (in instanceof JumpEqualConstantInstruction j && j.getTarget() != null) {
                labels.add(j.getTarget());
            }
            if (in instanceof JumpEqualVariableInstruction j && j.getTarget() != null) {
                labels.add(j.getTarget());
            }
        }
        // אם תרצה להציג EXIT תמיד, אפשר להוסיף:
        // labels.add(FixedLabel.EXIT);
        return labels;
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
                    if (ea && !eb) return 1;       // EXIT תמיד בסוף
                    if (!ea && eb) return -1;
                    return a.getLabelRepresentation().compareTo(b.getLabelRepresentation());
                })
                .map(semulator.label.Label::getLabelRepresentation)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private static String labelBox(semulator.label.Label l) {
        if (l == null) return "[     ]";
        return l.getLabelRepresentation();
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
