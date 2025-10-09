package com.semulator.engine.model;

import com.semulator.engine.exec.ExecutionContext;

public class AssignConstantInstruction extends AbstractInstruction {

    private final long constant;

    public AssignConstantInstruction(Variable target, long constant) {
        super(InstructionData.ASSIGN_CONSTANT, target);
        this.constant = constant;
    }

    public AssignConstantInstruction(Variable target, long constant, Label label) {
        super(InstructionData.ASSIGN_CONSTANT, target, label);
        this.constant = constant;
    }

    @Override
    public Label execute(ExecutionContext context) {
        context.updateVariable(getVariable(), constant);
        return FixedLabel.EMPTY;
    }

    public long getConstant() {
        return constant;
    }
}
