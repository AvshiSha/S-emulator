package ui;

import semulator.instructions.*;
import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.label.LabelImpl;
import semulator.variable.Variable;
import semulator.variable.VariableImpl;
import semulator.variable.VariableType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TempProgramFactory {

    // מחלקת מודל פשוטה (לא record)
    public static final class TempProgram {
        private final String name;
        private final List<SInstruction> instructions;
        private final Set<String> inputsUsed;
        private final List<Label> labelsUsed;

        public TempProgram(String name,
                List<SInstruction> instructions,
                Set<String> inputsUsed,
                List<Label> labelsUsed) {
            if (name == null)
                name = "unnamed-program";
            this.name = name;
            // עושים העתק הגנתי כדי שלא ישנו לנו מבחוץ
            this.instructions = new ArrayList<>(instructions);
            this.inputsUsed = new HashSet<>(inputsUsed);
            this.labelsUsed = new ArrayList<>(labelsUsed);
        }

        public String getName() {
            return name;
        }

        public List<SInstruction> getInstructions() {
            return new ArrayList<>(instructions);
        }

        public Set<String> getInputsUsed() {
            return new HashSet<>(inputsUsed);
        }

        public List<Label> getLabelsUsed() {
            return new ArrayList<>(labelsUsed);
        }
    }

    private TempProgramFactory() {
    }

    /**
     * בונה תוכנית דמו קטנה עם:
     * L1: z1 <- 0
     * y <- z1
     * y <- y + 1
     * IF x1 != 0 GOTO L2
     * GOTO L1
     */
    public static TempProgram sample() {
        // --- משתנים ---
        Variable y = Variable.RESULT;
        Variable x1 = new VariableImpl(VariableType.INPUT, 1);
        Variable z1 = new VariableImpl(VariableType.WORK, 1);

        // --- לייבלים (address לא חשוב ל-Show) ---
        Label L1 = new LabelImpl(1, 0);
        Label L2 = new LabelImpl(2, 0);

        // --- הוראות ---
        List<SInstruction> prog = List.of(
                new ZeroVariableInstruction(z1, L1), // [L1] z1 <- 0
                new AssignVariableInstruction(y, z1), // y <- z1
                new IncreaseInstruction(y), // y <- y + 1
                new JumpNotZeroInstruction(x1, FixedLabel.EMPTY, L2), // IF x1 != 0 GOTO L2
                new GotoLabelInstruction(L1) // GOTO L1
        );

        // --- קלטים ולייבלים בשימוש ---
        Set<String> inputs = Set.of("x1");
        List<Label> labels = List.of(L1, L2 /* , FixedLabel.EXIT אם תרצה להדגים */);

        return new TempProgram("demo-program", prog, inputs, labels);
    }
}
