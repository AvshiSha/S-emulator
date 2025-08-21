package semulator.instructions;

import semulator.execution.ExecutionContext;
import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.variable.Variable;

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
