package ui;

import semulator.instructions.*;
import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.program.ExpansionResult;
import semulator.program.SProgram;

import java.util.*;
import java.util.stream.Collectors;

public final class PrettyPrinter {
    private PrettyPrinter() {
    }

    public static String show(SProgram p) {
        StringBuilder sb = new StringBuilder();

        List<SInstruction> ins = p.getInstructions();

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

    private static Set<String> deriveInputs(List<SInstruction> ins) {
        Set<String> xs = new HashSet<>();
        for (SInstruction in : ins) {
            if (in.getVariable() != null) {
                String v = in.getVariable().toString();
                if (v.startsWith("x"))
                    xs.add(v);
            }
            if (in instanceof AssignVariableInstruction a && a.getSource() != null) {
                String s = a.getSource().toString();
                if (s.startsWith("x"))
                    xs.add(s);
            }
            if (in instanceof JumpEqualVariableInstruction j && j.getOther() != null) {
                String o = j.getOther().toString();
                if (o.startsWith("x"))
                    xs.add(o);
            }
        }
        return xs;
    }

    public static void printTopicInputs(SProgram p) {
        StringBuilder sb = new StringBuilder();
        List<SInstruction> ins = p.getInstructions();
        Set<String> inputsUsed = deriveInputs(ins);
        List<Label> labelsForHeader = uniqueLabelsForHeader(ins);
        System.out.println("Program: " + p.getName());
        sb.append("Inputs: ").append(formatInputs(inputsUsed)).append("\n");
        sb.append("Labels: ")
                .append(labelsForHeader.stream()
                        .map(l -> l.isExit() ? "EXIT" : l.getLabel())
                        .collect(Collectors.joining(", ")))
                .append("\n");
        System.out.println(sb.toString());
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
        if (name == null)
            name = "";

        final int WIDTH = 6; // Fixed width for label box including brackets
        if (name.length() > WIDTH - 2) // -2 for the brackets
            name = name.substring(0, WIDTH - 2);

        if (name.isEmpty()) {
            return " ".repeat(WIDTH); // Fixed width empty space
        }

        // Center the label within the box
        int totalWidth = WIDTH;
        int labelWidth = name.length() + 2; // +2 for brackets
        int padding = totalWidth - labelWidth;
        int leftPadding = padding / 2;
        int rightPadding = padding - leftPadding;

        return " ".repeat(leftPadding) + "[" + name + "]" + " ".repeat(rightPadding);
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
        } else if (in instanceof QuoteInstruction q) {
            String arguments = "";
            List<FunctionArgument> args = q.getFunctionArguments();
            for (int i = 0; i < args.size(); i++) {
                arguments += args.get(i).toString();
                if (i < args.size() - 1) {
                    arguments += ",";
                }
            }
            return q.getVariable() + " <- (" + q.getFunctionName() + ", " + arguments + ")";
        } else if (in instanceof JumpEqualFunctionInstruction jef) {
            String arguments = "";
            List<FunctionArgument> args = jef.getFunctionArguments();
            for (int i = 0; i < args.size(); i++) {
                arguments += args.get(i).toString();
                if (i < args.size() - 1) {
                    arguments += ",";
                }
            }
            return "IF " + jef.getVariable() + " == (" + jef.getFunctionName() + ", " + arguments + ") GOTO "
                    + jef.getTarget();
        }
        return in.getName();
    }

    // לייבלים לכותרת — ייחודיים, לא ריקים, ואם EXIT הופיע איפשהו — הוא יופיע פעם
    // אחת ובסוף
    private static List<semulator.label.Label> uniqueLabelsForHeader(List<SInstruction> ins) {
        boolean sawExit = false;
        java.util.Map<String, Label> uniq = new LinkedHashMap<>();

        for (semulator.instructions.SInstruction in : ins) {
            // לייבל שמוגדר על ההוראה
            var self = in.getLabel();
            if (self != null) {
                if (self.isExit())
                    sawExit = true;
                else
                    putIfHasName(uniq, self);
            }
            // יעדי קפיצה/בדיקות – כיסוי הסוגים הרלוונטיים
            if (in instanceof semulator.instructions.GotoLabelInstruction g && g.getTarget() != null) {
                if (g.getTarget().isExit())
                    sawExit = true;
                else
                    putIfHasName(uniq, g.getTarget());
            }
            if (in instanceof semulator.instructions.JumpNotZeroInstruction j && j.getTarget() != null) {
                if (j.getTarget().isExit())
                    sawExit = true;
                else
                    putIfHasName(uniq, j.getTarget());
            }
            if (in instanceof semulator.instructions.JumpZeroInstruction j && j.getTarget() != null) {
                if (j.getTarget().isExit())
                    sawExit = true;
                else
                    putIfHasName(uniq, j.getTarget());
            }
            if (in instanceof semulator.instructions.JumpEqualConstantInstruction j && j.getTarget() != null) {
                if (j.getTarget().isExit())
                    sawExit = true;
                else
                    putIfHasName(uniq, j.getTarget());
            }
            if (in instanceof semulator.instructions.JumpEqualVariableInstruction j && j.getTarget() != null) {
                if (j.getTarget().isExit())
                    sawExit = true;
                else
                    putIfHasName(uniq, j.getTarget());
            }
        }

        List<Label> out = new ArrayList<>(uniq.values());
        if (sawExit)
            out.add(semulator.label.FixedLabel.EXIT); // EXIT פעם אחת ובסוף
        return out;
    }

    private static void putIfHasName(Map<String, semulator.label.Label> uniq,
            semulator.label.Label lbl) {
        String name = lbl.toString();
        if (name != null && !name.isBlank()) {
            uniq.putIfAbsent(name, lbl);
        }
    }

    // === NEW: pretty-print a snapshot with lineage (<<< creators), nicely aligned
    // ===
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
        int numWidth = digits(ins.size()); // width for "#N"
        int labelInnerW = Math.max(4, maxLabelInnerWidth(all)); // text inside [ ]
        int textWidth = Math.max(16, maxTextWidth(all)); // instruction string
        int cyclesWidth = Math.max(1, maxCyclesWidth(all));

        // 3) Print each row + its creator chain with the same row number and aligned
        // columns
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

    // === NEW: Show creation chains grouped by degree ===
    // Shows instructions grouped by degree with complete creation chains
    // Left: Highest degree (user's chosen), Right: Degree 0 (original program)
    // Format: #5 (S) [ ] y <- 0 (1) >>> #3 (S) [ ] y <- 5 (2) >>> #1 (S) [ L4 ] IF
    // y = 5 GOTO L5 (2)
    public static String showCreationChains(ExpansionResult r, SProgram originalProgram) {
        StringBuilder sb = new StringBuilder();

        List<SInstruction> prog = r.instructions(); // final snapshot program order
        Map<SInstruction, SInstruction> parent = r.parent(); // immediate parent links
        Map<SInstruction, Integer> lineNo = r.lineNo(); // row numbers for each instruction

        // Precompute depth for any instruction we might print (finals + ancestors)
        Map<SInstruction, Integer> depthCache = new IdentityHashMap<>();
        java.util.function.Function<SInstruction, Integer> depthFn = ins -> {
            Integer d = depthCache.get(ins);
            if (d != null)
                return d;
            int dd = 0;
            SInstruction cur = ins;
            while (true) {
                SInstruction p = parent.get(cur);
                if (p == null)
                    break;
                dd++;
                cur = p;
            }
            depthCache.put(ins, dd);
            return dd;
        };

        // Collect all instructions that *might* be printed for width calc
        Set<SInstruction> allToMeasure = Collections.newSetFromMap(new IdentityHashMap<>());
        allToMeasure.addAll(prog);
        for (SInstruction in : prog) {
            SInstruction cur = in;
            while ((cur = parent.get(cur)) != null) {
                allToMeasure.add(cur);
            }
        }

        // Compute widths once
        int numWidth = Math.max(1, String.valueOf(Math.max(1, prog.size() + 32)).length());
        int labelInnerW = Math.max(4, maxLabelInnerWidth(allToMeasure));
        int textWidth = Math.max(16, maxTextWidth(allToMeasure));
        int cyclesWidth = Math.max(1, maxCyclesWidth(allToMeasure));

        // Find the maximum depth (highest degree)
        int maxDepth = 0;
        for (SInstruction ins : prog) {
            maxDepth = Math.max(maxDepth, depthFn.apply(ins));
        }

        // Only print instructions from the highest degree (maxDepth)
        List<SInstruction> highestDegreeInstructions = new ArrayList<>();
        for (SInstruction instruction : prog) {
            int depth = depthFn.apply(instruction);
            if (depth == maxDepth) {
                highestDegreeInstructions.add(instruction);
            }
        }

        // Sort instructions by their final line number for consistent ordering
        highestDegreeInstructions.sort((a, b) -> Integer.compare(
                lineNo.getOrDefault(a, 0),
                lineNo.getOrDefault(b, 0)));

        // Print only the highest degree instructions with their creation chains
        for (SInstruction ins : highestDegreeInstructions) {
            // Build the complete creation chain for this instruction
            List<SInstruction> chain = new ArrayList<>();
            SInstruction current = ins;

            // Add the instruction itself and all its ancestors
            while (current != null) {
                chain.add(current);
                current = parent.get(current);
            }

            for (int i = 0; i < chain.size(); i++) {
                if (i > 0) {
                    sb.append(" >>> ");
                }

                SInstruction chainIns = chain.get(i);
                Integer displayNum = lineNo.get(chainIns);

                // For the highest degree instruction (i=0), use lineNo
                // For ancestor instructions (i>0), they don't exist in lineNo
                if (displayNum == null) {
                    // This is an ancestor instruction - find its position in the original program
                    if (i == chain.size() - 1) {
                        // This is a degree 0 instruction - find its original position
                        List<SInstruction> originalInstructions = originalProgram.getInstructions();
                        for (int j = 0; j < originalInstructions.size(); j++) {
                            if (originalInstructions.get(j) == chainIns) {
                                displayNum = j + 1; // Convert to 1-based
                                break;
                            }
                        }
                        if (displayNum == null) {
                            displayNum = i + 1; // Fallback
                        }
                    } else {
                        // This is an intermediate degree instruction - use chain position
                        displayNum = i + 1;
                    }
                }

                sb.append(oneLineAligned(displayNum, chainIns, numWidth, labelInnerW,
                        textWidth, cyclesWidth));
            }
            sb.append('\n');
        }

        return sb.toString();
    }

    // === NEW: row-major pretty print for expanded snapshots ===
    // Prints one physical line per original row.
    // Within that line, prints the chain of descendants for that row from newest
    // (left) to oldest (right),
    // each segment formatted as "#<baseRow+depth> (B|S) [LABEL] TEXT (cycles)",
    // with segments separated by " <<< ".
    public static String showRowMajor(ExpansionResult r) {
        StringBuilder sb = new StringBuilder();

        // Pull data from the snapshot
        List<SInstruction> prog = r.instructions(); // final snapshot program order
        Map<SInstruction, SInstruction> parent = r.parent(); // immediate parent links
        Map<SInstruction, Integer> lineNo = r.lineNo(); // final numbering 1..N
        Map<SInstruction, Integer> rowOf = r.rowOf(); // NEW: base row (0-based) for each instruction

        if (rowOf == null || rowOf.isEmpty()) {
            // Fallback: no row info -> default to showWithCreators
            return showWithCreators(r);
        }

        // Group final instructions by their original row
        Map<Integer, List<SInstruction>> finalsByRow = new HashMap<>();
        for (SInstruction ins : prog) {
            Integer row = rowOf.get(ins);
            if (row == null) {
                // If some instruction lacks row info, approximate with its final lineNo-1
                row = lineNo.getOrDefault(ins, 1) - 1;
            }
            finalsByRow.computeIfAbsent(row, k -> new ArrayList<>()).add(ins);
        }

        // Precompute depth for any instruction we might print (finals + ancestors)
        Map<SInstruction, Integer> depthCache = new IdentityHashMap<>();
        java.util.function.Function<SInstruction, Integer> depthFn = ins -> {
            Integer d = depthCache.get(ins);
            if (d != null)
                return d;
            int dd = 0;
            SInstruction cur = ins;
            while (true) {
                SInstruction p = parent.get(cur);
                if (p == null)
                    break;
                dd++;
                cur = p;
            }
            depthCache.put(ins, dd);
            return dd;
        };

        // Collect all instructions that *might* be printed for width calc
        Set<SInstruction> allToMeasure = Collections.newSetFromMap(new IdentityHashMap<>());
        allToMeasure.addAll(prog);
        for (SInstruction in : prog) {
            SInstruction cur = in;
            while ((cur = parent.get(cur)) != null) {
                allToMeasure.add(cur);
            }
        }

        // Compute widths once
        int numWidth = Math.max(1, String.valueOf(Math.max(1, prog.size() + 32)).length()); // safe upper bound
        int labelInnerW = Math.max(4, maxLabelInnerWidth(allToMeasure));
        int textWidth = Math.max(16, maxTextWidth(allToMeasure));
        int cyclesWidth = Math.max(1, maxCyclesWidth(allToMeasure));

        // Render each base row
        List<Integer> rows = new ArrayList<>(finalsByRow.keySet());
        Collections.sort(rows);

        for (int rowId : rows) {
            // 1) Build the UNION set for this row: finals + all their ancestors
            Set<SInstruction> set = Collections.newSetFromMap(new IdentityHashMap<>());
            for (SInstruction f : finalsByRow.get(rowId)) {
                SInstruction cur = f;
                set.add(cur);
                while ((cur = parent.get(cur)) != null) {
                    set.add(cur);
                }
            }

            // 2) Order newest→oldest (depth desc), tie-break by final program order for
            // determinism
            List<SInstruction> ordered = new ArrayList<>(set);
            ordered.sort((a, b) -> {
                int da = depthFn.apply(a);
                int db = depthFn.apply(b);
                if (da != db)
                    return Integer.compare(db, da); // larger depth first
                return Integer.compare(lineNo.getOrDefault(a, 0), lineNo.getOrDefault(b, 0));
            });

            // 3) Assign UNIQUE, sequential display row numbers left→right:
            // leftmost = baseRow+count, ... rightmost (original) = baseRow+1
            int count = ordered.size();
            for (int i = 0; i < count; i++) {
                if (i > 0)
                    sb.append(" <<< ");
                int displayRow = (rowId + 1) + (count - 1 - i);
                sb.append(oneSegmentAligned(displayRow, ordered.get(i),
                        numWidth, labelInnerW, textWidth, cyclesWidth));
            }
            sb.append('\n');
        }

        return sb.toString();
    }

    // compute distance child->...->root via parent map
    private static int depth(SInstruction ins, Map<SInstruction, SInstruction> parent) {
        int d = 0;
        SInstruction cur = ins;
        while (true) {
            SInstruction p = parent.get(cur);
            if (p == null)
                break;
            d++;
            cur = p;
        }
        return d;
    }

    // Format one segment with custom row number (aligned)
    private static String oneSegmentAligned(int displayRow,
            SInstruction in,
            int numW, int lblInnerW, int textW, int cycW) {
        String n = String.format("%" + numW + "d", displayRow);
        String kind = kindLetter(in); // "B" or "S"
        String label = labelBoxFixed(in.getLabel(), lblInnerW);
        String text = renderInstruction(in);
        return String.format("#%s (%s) %s %-" + textW + "s (%" + cycW + "d)",
                n, kind, label, text, in.cycles());
    }

    // Renders a single aligned line: "#NN (K) [ LABEL ] TEXT (cycles)"
    private static String oneLineAligned(Integer num,
            SInstruction in,
            int numW, int lblInnerW, int textW, int cycW) {
        String n = (num == null ? "?".repeat(Math.max(1, numW)) : String.format("%" + numW + "d", num));
        String kind = kindLetter(in); // "B" or "S"
        String label = labelBoxFixed(in.getLabel(), lblInnerW);
        String text = renderInstruction(in);

        return String.format("#%s (%s) %s %-" + textW + "s (%" + cycW + "d)",
                n, kind, label, text, in.cycles());
    }

    // Fixed-width label box with centered inner text, e.g., "[ L2 ]" or "[ EXIT ]"
    private static String labelBoxFixed(semulator.label.Label l, int innerWidth) {
        String name = null;
        if (l != null)
            name = l.isExit() ? "EXIT" : l.getLabel();
        if (name == null)
            name = "";
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
            if (name == null)
                name = "";
            if (name.length() > m)
                m = name.length();
        }
        return m;
    }

    private static int maxTextWidth(java.util.Set<SInstruction> all) {
        int m = 0;
        for (SInstruction in : all) {
            int len = renderInstruction(in).length();
            if (len > m)
                m = len;
        }
        return m;
    }

    private static int maxCyclesWidth(java.util.Set<SInstruction> all) {
        int m = 1;
        for (SInstruction in : all) {
            int len = String.valueOf(in.cycles()).length();
            if (len > m)
                m = len;
        }
        return m;
    }

}
