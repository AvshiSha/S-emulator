package semulator.instructions;

import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.variable.Variable;

public abstract class AbstractInstruction implements SInstruction {

    private final InstructionData instructionData;
    private final Label label;
    private final Variable variable;

    public AbstractInstruction(InstructionData instructionData, Variable variable) {
        this(instructionData, variable, FixedLabel.EMPTY);
    }

    public AbstractInstruction(InstructionData instructionData, Variable variable, Label label) {
        if (instructionData == null) throw new IllegalArgumentException("instructionData is null");
        if (variable == null)        throw new IllegalArgumentException("variable is null");
        if (label == null)           throw new IllegalArgumentException("label is null");
        this.instructionData = instructionData;
        this.label = label;
        this.variable = variable;
    }

    @Override
    public String getName() {
        return instructionData.getName();
    }

    @Override
    public int cycles() {
        return instructionData.getCycles();
    }

    @Override
    public Label getLabel() {
        return label;
    }

    @Override
    public Variable getVariable() {
        return variable;
    }
}