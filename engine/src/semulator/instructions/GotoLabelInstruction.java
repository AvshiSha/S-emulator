package semulator.instructions;

import semulator.execution.ExecutionContext;
import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.variable.Variable;

public class GotoLabelInstruction extends AbstractInstruction {

    private final Label target;

    public GotoLabelInstruction(Label target) {
        super(InstructionData.GOTO_LABEL, Variable.RESULT); // dummy
        if (target == null) throw new IllegalArgumentException("target is null");
        this.target = target;
    }

    public GotoLabelInstruction(Label label, Label target) {
        super(InstructionData.GOTO_LABEL, Variable.RESULT, label); // dummy
        if (target == null) throw new IllegalArgumentException("target is null");
        this.target = target;
    }

    @Override
    public Label execute(ExecutionContext context) {
        return target; // קפיצה ללא תנאי
    }

    public Label getTarget() {
        return target;
    }

}
