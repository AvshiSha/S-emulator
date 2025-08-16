package semulator.instructions;

import semulator.execution.ExecutionContext;
import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.variable.Variable;

public class NoOpInstruction extends AbstractInstruction {

    public NoOpInstruction(Variable variable) {
        super(InstructionData.NO_OP, variable);
    }

    public NoOpInstruction(Variable variable, Label label) {
        super(InstructionData.NO_OP, variable, label);
    }

    @Override
    public Label execute(ExecutionContext context) {
        return FixedLabel.EMPTY;

    }
}
