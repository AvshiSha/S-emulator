package semulator.instructions;

import semulator.execution.ExecutionContext;
import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.variable.Variable;

public class JumpEqualConstantInstruction extends AbstractInstruction {

    private final long constant;
    private final Label target;

    public JumpEqualConstantInstruction(Variable v, long constant, Label target) {
        super(InstructionData.JUMP_EQ_CONSTANT, v);
        this.constant = constant;
        if (target == null) throw new IllegalArgumentException("target is null");
        this.target = target;
    }

    public JumpEqualConstantInstruction(Variable v, Label label, long constant, Label target) {
        super(InstructionData.JUMP_EQ_CONSTANT, v, label);
        this.constant = constant;
        if (target == null) throw new IllegalArgumentException("target is null");
        this.target = target;
    }

    @Override
    public Label execute(ExecutionContext context) {
        long val = context.getVariableValue(getVariable());
        return (val == constant) ? target : FixedLabel.EMPTY;
    }
}
