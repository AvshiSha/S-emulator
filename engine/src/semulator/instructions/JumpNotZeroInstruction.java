package semulator.instructions;

import semulator.execution.ExecutionContext;
import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.variable.Variable;

public class JumpNotZeroInstruction extends AbstractInstruction {

    private final Label jnzLabel;

    public JumpNotZeroInstruction(Variable variable, Label jnzLabel) {
        this(variable, jnzLabel, FixedLabel.EMPTY);
    }

    public JumpNotZeroInstruction(Variable variable, Label jnzLabel, Label label) {
        super(InstructionData.JUMP_NOT_ZERO, variable, label);
        this.jnzLabel = jnzLabel;
    }

    @Override
    public Label execute(ExecutionContext context) {
        long variableValue = context.getVariableValue(getVariable());

        if (variableValue != 0) {
            return jnzLabel;
        }
        return FixedLabel.EMPTY;

    }
}
