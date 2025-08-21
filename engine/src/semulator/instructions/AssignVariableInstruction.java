package semulator.instructions;

import semulator.execution.ExecutionContext;
import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.variable.Variable;

public class AssignVariableInstruction extends AbstractInstruction {

    private final Variable source;


    public AssignVariableInstruction(Variable target, Variable source) {
        super(InstructionData.ASSIGN_VARIABLE, target);
        if (source == null) throw new IllegalArgumentException("source is null");
        this.source = source;
    }

    public AssignVariableInstruction(Variable target, Variable source, Label label) {
        super(InstructionData.ASSIGN_VARIABLE, target, label);
        if (source == null) throw new IllegalArgumentException("source is null");
        this.source = source;
    }

    @Override
    public Label execute(ExecutionContext context) {
        long v = context.getVariableValue(source);
        context.updateVariable(getVariable(), v);
        return FixedLabel.EMPTY;
    }

    public semulator.variable.Variable getSource() {
        return source;
    }

}
