package com.semulator.engine.model;

import com.semulator.engine.exec.ExecutionContext;

public class JumpZeroInstruction extends AbstractInstruction {

    private final Label target;

    public JumpZeroInstruction(Variable v, Label target) {
        super(InstructionData.JUMP_ZERO, v);
        if (target == null)
            throw new IllegalArgumentException("target is null");
        this.target = target;
    }

    public JumpZeroInstruction(Variable v, Label label, Label target) {
        super(InstructionData.JUMP_ZERO, v, label);
        if (target == null)
            throw new IllegalArgumentException("target is null");
        this.target = target;
    }

    public Label getTarget() {
        return target;
    }

    @Override
    public Label execute(ExecutionContext context) {
        long val = context.getVariableValue(getVariable());
        return (val == 0L) ? target : FixedLabel.EMPTY;
    }
}
