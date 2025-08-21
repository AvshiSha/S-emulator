package semulator.instructions;

import semulator.execution.ExecutionContext;
import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.variable.Variable;

public class JumpEqualVariableInstruction extends AbstractInstruction {

    private final Variable other;
    private final Label target;

    public JumpEqualVariableInstruction(Variable v, Variable other, Label target) {
        super(InstructionData.JUMP_EQ_VARIABLE, v);
        if (other == null) throw new IllegalArgumentException("other is null");
        if (target == null) throw new IllegalArgumentException("target is null");
        this.other = other;
        this.target = target;
    }

    public JumpEqualVariableInstruction(Variable v, Label label, Variable other, Label target) {
        super(InstructionData.JUMP_EQ_VARIABLE, v, label);
        if (other == null) throw new IllegalArgumentException("other is null");
        if (target == null) throw new IllegalArgumentException("target is null");
        this.other = other;
        this.target = target;
    }

    public Variable getOther() {
        return other;
    }

    public Label getTarget() {
        return target;
    }

    @Override
    public Label execute(ExecutionContext context) {
        long a = context.getVariableValue(getVariable());
        long b = context.getVariableValue(other);
        return (a == b) ? target : FixedLabel.EMPTY;
    }
}
