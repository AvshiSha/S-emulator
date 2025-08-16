package semulator.instructions;


import semulator.execution.ExecutionContext;
import semulator.label.Label;
import semulator.variable.Variable;

public interface SInstruction {

    String getName();

    Label execute(ExecutionContext context);

    int cycles();

    Label getLabel();

    Variable getVariable();
}
