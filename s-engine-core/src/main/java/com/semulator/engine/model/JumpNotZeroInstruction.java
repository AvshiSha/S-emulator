package com.semulator.engine.model;

import com.semulator.engine.exec.ExecutionContext;

public class JumpNotZeroInstruction extends AbstractInstruction {

    private final Label jnzLabel; // יעד הקפיצה

    public JumpNotZeroInstruction(Variable variable, Label jnzLabel) {
        this(variable, FixedLabel.EMPTY, jnzLabel);
    }

    public JumpNotZeroInstruction(Variable variable, Label label, Label jnzLabel) {
        super(InstructionData.JUMP_NOT_ZERO, variable, label);
        if (jnzLabel == null)
            throw new IllegalArgumentException("target label is null");
        this.jnzLabel = jnzLabel;
    }

    /** נדרש ל-PrettyPrinter */
    public Label getTarget() {
        return jnzLabel;
    }

    @Override
    public Label execute(ExecutionContext context) {
        long variableValue = context.getVariableValue(getVariable());
        return (variableValue != 0) ? jnzLabel : FixedLabel.EMPTY;
    }
}
