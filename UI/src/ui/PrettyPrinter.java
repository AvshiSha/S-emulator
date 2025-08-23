// ui/PrettyPrinter.java
package ui;

import semulator.instructions.*;
import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.program.SProgram;   // <-- חדש

import java.util.*;
import java.util.stream.Collectors;

public final class PrettyPrinter {
    private PrettyPrinter() {
    }

    // === חדש: תצוגה של תוכנית אמיתית (SProgram) ===
    public static String show(SProgram p) {
        StringBuilder sb = new StringBuilder();

        // כותרות
        sb.append("Program: ").append(p.getName()).append("\n");

        // הפקה דינמית של Inputs/Labels מתוך ההוראות הקיימות
        List<SInstruction> ins = p.getInstructions();
        Set<String> inputsUsed = deriveInputs(ins);
        List<Label> labelsForHeader = uniqueLabelsForHeader(ins);

        sb.append("Inputs: ").append(formatInputs(inputsUsed)).append("\n");
        sb.append("Labels: ")
                .append(labelsForHeader.stream()
                        .map(l -> l.isExit() ? "EXIT" : l.getLabel())
                        .collect(Collectors.joining(", ")))
                .append("\n\n");

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

    private static String formatInputs(Set<String> inputs) {
        return inputs.stream()
                .sorted(Comparator.comparingInt(PrettyPrinter::xIndex))
                .collect(Collectors.joining(", "));
    }

    private static int xIndex(String x) {
        try {
            return Integer.parseInt(x.substring(1));
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    private static String labelBox(semulator.label.Label l) {
        String name = null;
        if (l != null) {
            name = l.isExit() ? "EXIT" : l.getLabel();
        }
        if (name == null) name = "";

        final int WIDTH = 5;
        if (name.length() > WIDTH) name = name.substring(0, WIDTH);

        int pad = WIDTH - name.length();
        // ריווח לאמצע: כשיש שארית, נוסיף את הרווח העודף לשמאל כדי לקבל "[  L1 ]"
        int left = pad / 2;
        int right = pad - left;

        if (name.isEmpty()) return "    ";

        return "[" + name + "]";
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

    // לייבלים לכותרת — ייחודיים, לא ריקים, ואם EXIT הופיע איפשהו — הוא יופיע פעם אחת ובסוף
    private static java.util.List<semulator.label.Label> uniqueLabelsForHeader(java.util.List<semulator.instructions.SInstruction> ins) {
        boolean sawExit = false;
        java.util.Map<String, semulator.label.Label> uniq = new java.util.LinkedHashMap<>();

        for (semulator.instructions.SInstruction in : ins) {
            // לייבל שמוגדר על ההוראה
            var self = in.getLabel();
            if (self != null) {
                if (self.isExit()) sawExit = true;
                else putIfHasName(uniq, self);
            }
            // יעדי קפיצה/בדיקות – כיסוי הסוגים הרלוונטיים
            if (in instanceof semulator.instructions.GotoLabelInstruction g && g.getTarget() != null) {
                if (g.getTarget().isExit()) sawExit = true;
                else putIfHasName(uniq, g.getTarget());
            }
            if (in instanceof semulator.instructions.JumpNotZeroInstruction j && j.getTarget() != null) {
                if (j.getTarget().isExit()) sawExit = true;
                else putIfHasName(uniq, j.getTarget());
            }
            if (in instanceof semulator.instructions.JumpZeroInstruction j && j.getTarget() != null) {
                if (j.getTarget().isExit()) sawExit = true;
                else putIfHasName(uniq, j.getTarget());
            }
            if (in instanceof semulator.instructions.JumpEqualConstantInstruction j && j.getTarget() != null) {
                if (j.getTarget().isExit()) sawExit = true;
                else putIfHasName(uniq, j.getTarget());
            }
            if (in instanceof semulator.instructions.JumpEqualVariableInstruction j && j.getTarget() != null) {
                if (j.getTarget().isExit()) sawExit = true;
                else putIfHasName(uniq, j.getTarget());
            }
        }

        java.util.List<semulator.label.Label> out = new java.util.ArrayList<>(uniq.values());
        if (sawExit) out.add(semulator.label.FixedLabel.EXIT); // EXIT פעם אחת ובסוף
        return out;
    }

    private static void putIfHasName(java.util.Map<String, semulator.label.Label> uniq,
                                     semulator.label.Label lbl) {
        String name = lbl.toString();
        if (name != null && !name.isBlank()) {
            uniq.putIfAbsent(name, lbl);
        }
    }

    // === NEW: pretty-print a snapshot with lineage (<<< creators) ===
    public static String showWithCreators(semulator.program.ExpansionResult r) {
        StringBuilder sb = new StringBuilder();

        List<SInstruction> ins = r.instructions();
        Map<SInstruction, SInstruction> parent = r.parent();
        Map<SInstruction, Integer> lineNo = r.lineNo();

        for (int i = 0; i < ins.size(); i++) {
            SInstruction in = ins.get(i);

            // Main line
            String main = oneLine(lineNo.get(in), in);
            sb.append(main);

            // Follow the creator chain: child -> parent -> grandparent -> ...
            SInstruction cur = in;
            while (parent.containsKey(cur)) {
                SInstruction p = parent.get(cur);
                sb.append(" <<< ").append(oneLine(lineNo.get(p), p));
                cur = p;
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // Renders a single instruction exactly like your show(...) body does for one row
    private static String oneLine(Integer num, SInstruction in) {
        String n = (num == null ? "?" : String.valueOf(num));
        String kind = kindLetter(in);
        String labelBox = labelBox(in.getLabel());
        String text = renderInstruction(in);
        int cycles = in.cycles();
        return String.format("#%s (%s) %s %s (%d)", n, kind, labelBox, text, cycles);
    }


}
