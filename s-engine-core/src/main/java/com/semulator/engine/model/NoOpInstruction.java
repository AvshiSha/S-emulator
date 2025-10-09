package com.semulator.engine.model;

import com.semulator.engine.exec.ExecutionContext;

public class NoOpInstruction extends AbstractInstruction {

    public NoOpInstruction(Variable variable) {
        super(InstructionData.NEUTRAL, variable);
    }

    public NoOpInstruction(Variable variable, Label label) {
        super(InstructionData.NEUTRAL, variable, label);
    }

    @Override
    public Label execute(ExecutionContext context) {
        return FixedLabel.EMPTY;

    }
}
