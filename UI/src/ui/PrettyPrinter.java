// ui/PrettyPrinter.java
package ui;

import semulator.instructions.*;
import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.program.ExpansionResult;
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
    private static List<semulator.label.Label> uniqueLabelsForHeader(List<SInstruction> ins) {
        boolean sawExit = false;
        java.util.Map<String, Label> uniq = new LinkedHashMap<>();

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

        List<Label> out = new ArrayList<>(uniq.values());
        if (sawExit) out.add(semulator.label.FixedLabel.EXIT); // EXIT פעם אחת ובסוף
        return out;
    }

    private static void putIfHasName(Map<String, semulator.label.Label> uniq,
                                     semulator.label.Label lbl) {
        String name = lbl.toString();
        if (name != null && !name.isBlank()) {
            uniq.putIfAbsent(name, lbl);
        }
    }

    // === NEW: pretty-print a snapshot with lineage (<<< creators), nicely aligned ===
    public static String showWithCreators(ExpansionResult r) {
        StringBuilder sb = new StringBuilder();

        List<SInstruction> ins = r.instructions();
        Map<SInstruction, SInstruction> parent = r.parent();
        Map<SInstruction, Integer> lineNo = r.lineNo();

        // 1) Collect all instructions we might print (mains + entire creator chains)
        Set<SInstruction> all = Collections.newSetFromMap(new IdentityHashMap<>());
        for (SInstruction in : ins) {
            SInstruction cur = in;
            all.add(cur);
            while (parent.containsKey(cur)) {
                cur = parent.get(cur);
                all.add(cur);
            }
        }

        // 2) Measure column widths for this snapshot
        int numWidth = digits(ins.size());             // width for "#N"
        int labelInnerW = Math.max(4, maxLabelInnerWidth(all)); // text inside [     ]
        int textWidth = Math.max(16, maxTextWidth(all));      // instruction string
        int cyclesWidth = Math.max(1, maxCyclesWidth(all));

        // 3) Print each row + its creator chain with the same row number and aligned columns
        for (int i = 0; i < ins.size(); i++) {
            SInstruction in = ins.get(i);
            Integer thisNum = lineNo.get(in);

            // Main line
            sb.append(oneLineAligned(thisNum, in, numWidth, labelInnerW, textWidth, cyclesWidth));

            // Creator chain: child -> parent -> grandparent ...
            SInstruction cur = in;
            while (parent.containsKey(cur)) {
                SInstruction p = parent.get(cur);
                sb.append(" <<< ")
                        // IMPORTANT: keep the same row number for the whole chain
                        .append(oneLineAligned(thisNum, p, numWidth, labelInnerW, textWidth, cyclesWidth));
                cur = p;
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // Renders a single aligned line: "#NN (K) [ LABEL ] TEXT (cycles)"
    private static String oneLineAligned(Integer num,
                                         SInstruction in,
                                         int numW, int lblInnerW, int textW, int cycW) {
        String n = (num == null ? "?".repeat(Math.max(1, numW)) : String.format("%" + numW + "d", num));
        String kind = kindLetter(in);                       // "B" or "S"
        String label = labelBoxFixed(in.getLabel(), lblInnerW);
        String text = renderInstruction(in);
        // If you want ellipsis for a very long text, uncomment:
        // if (text.length() > textW) text = text.substring(0, Math.max(0, textW - 1)) + "…";
        return String.format("#%s (%s) %s %-" + textW + "s (%" + cycW + "d)",
                n, kind, label, text, in.cycles());
    }

    // Fixed-width label box with centered inner text, e.g., "[  L2  ]" or "[ EXIT ]"
    private static String labelBoxFixed(semulator.label.Label l, int innerWidth) {
        String name = null;
        if (l != null) name = l.isExit() ? "EXIT" : l.getLabel();
        if (name == null) name = "";
        // If empty -> same width as a boxed label ("[" + inner + "]") but all spaces
        if (name.isEmpty()) {
            return " ".repeat(innerWidth + 2);
        }
        if (name.length() > innerWidth) {
            name = name.substring(0, innerWidth);
        }
        int pad = innerWidth - name.length();
        int left = pad / 2;
        int right = pad - left;
        return "[" + " ".repeat(left) + name + " ".repeat(right) + "]";
    }

    // ------- width helpers (scan all instructions that may be printed) -------
    private static int digits(int n) {
        return String.valueOf(Math.max(1, n)).length();
    }

    private static int maxLabelInnerWidth(java.util.Set<SInstruction> all) {
        int m = 0;
        for (SInstruction in : all) {
            var l = in.getLabel();
            String name = (l == null ? "" : (l.isExit() ? "EXIT" : l.getLabel()));
            if (name == null) name = "";
            if (name.length() > m) m = name.length();
        }
        return m;
    }

    private static int maxTextWidth(java.util.Set<SInstruction> all) {
        int m = 0;
        for (SInstruction in : all) {
            int len = renderInstruction(in).length();
            if (len > m) m = len;
        }
        return m;
    }

    private static int maxCyclesWidth(java.util.Set<SInstruction> all) {
        int m = 1;
        for (SInstruction in : all) {
            int len = String.valueOf(in.cycles()).length();
            if (len > m) m = len;
        }
        return m;
    }


}
