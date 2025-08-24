// src/main/java/semulator/program/NameSession.java
package semulator.program;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import semulator.instructions.*;
import semulator.label.Label;
import semulator.label.LabelImpl;
import semulator.variable.Variable;
import semulator.variable.VariableImpl;
import semulator.variable.VariableType;

/**
 * Deterministic name generator for expansion runs.
 * Produces the smallest unused L# and z# names, skipping any already present.
 */
public final class NameSession {
    private final Set<String> usedLabelNames;
    private final Set<String> usedVarNames;
    private int lCursor = 1; // next candidate for "L" + lCursor
    private int zCursor = 1; // next candidate for "z" + zCursor

    /**
     * Start from already-known used names (from load/validate time).
     */
    public NameSession(Set<String> baseUsedLabelNames, Set<String> baseUsedVarNames) {
        this.usedLabelNames = new HashSet<>(baseUsedLabelNames);
        this.usedVarNames = new HashSet<>(baseUsedVarNames);
    }

    /**
     * Build a session by scanning an existing program snapshot.
     */
    public static NameSession fromProgram(List<SInstruction> ins) {
        Set<String> labels = new HashSet<>();
        Set<String> vars = new HashSet<>();
        for (SInstruction in : ins) {
            collectLabel(labels, in.getLabel());
            if (in instanceof GotoLabelInstruction g) collectLabel(labels, g.getTarget());
            if (in instanceof JumpNotZeroInstruction j) collectLabel(labels, j.getTarget());
            if (in instanceof JumpZeroInstruction j) collectLabel(labels, j.getTarget());
            if (in instanceof JumpEqualConstantInstruction j) collectLabel(labels, j.getTarget());
            if (in instanceof JumpEqualVariableInstruction j) collectLabel(labels, j.getTarget());

            collectVar(vars, in.getVariable());
            if (in instanceof AssignVariableInstruction a) collectVar(vars, a.getSource());
            if (in instanceof JumpEqualVariableInstruction j) collectVar(vars, j.getOther());
        }
        return new NameSession(labels, vars);
    }

    private static void collectLabel(Set<String> set, Label lbl) {
        if (lbl == null || lbl.isExit()) return;
        String s = lbl.getLabel();
        if (s != null && !s.isBlank() && s.charAt(0) == 'L') {
            set.add(s);
        }
    }

    private static void collectVar(Set<String> set, Variable v) {
        if (v == null) return;
        String s = v.toString();
        if (s != null && !s.isBlank() && s.charAt(0) == 'z') {
            set.add(s);
        }
    }

    /**
     * Returns a fresh label with name "L{n}", smallest n not used.
     */
    public Label freshLabel() {
        while (true) {
            String candidate = "L" + lCursor++;
            if (usedLabelNames.add(candidate)) {
                return new LabelImpl(candidate);
            }
        }
    }

    /**
     * Returns a fresh working variable with name "z{n}", smallest n not used.
     */
    public Variable freshZ() {
        while (true) {
            String candidate = "z" + zCursor++;
            if (usedVarNames.add(candidate)) {
                return new VariableImpl(VariableType.WORK,zCursor);
            }
        }
    }
}
