package semulator.instructions;

import semulator.execution.ExecutionContext;
import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.variable.Variable;

import java.util.List;

public class QuoteInstruction extends AbstractInstruction {

    private final String functionName;
    private final List<Variable> functionArguments;

    public QuoteInstruction(Variable target, String functionName, List<Variable> functionArguments) {
        super(InstructionData.QUOTE, target);
        if (functionName == null || functionName.trim().isEmpty()) {
            throw new IllegalArgumentException("functionName cannot be null or empty");
        }
        if (functionArguments == null) {
            throw new IllegalArgumentException("functionArguments cannot be null");
        }
        this.functionName = functionName.trim();
        this.functionArguments = functionArguments;
    }

    public QuoteInstruction(Variable target, String functionName, List<Variable> functionArguments, Label label) {
        super(InstructionData.QUOTE, target, label);
        if (functionName == null || functionName.trim().isEmpty()) {
            throw new IllegalArgumentException("functionName cannot be null or empty");
        }
        if (functionArguments == null) {
            throw new IllegalArgumentException("functionArguments cannot be null");
        }
        this.functionName = functionName.trim();
        this.functionArguments = functionArguments;
    }

    @Override
    public Label execute(ExecutionContext context) {
        // The QUOTE instruction should not be executed directly.
        // Instead, it should be expanded during program loading/expansion phase.
        // This method should never be called in normal execution flow.
        throw new UnsupportedOperationException("QUOTE instruction should be expanded before execution");
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<Variable> getFunctionArguments() {
        return functionArguments;
    }

    @Override
    public int cycles() {
        // QUOTE instructions have 5 base cycles plus the cycles of the quoted function
        // This will be calculated during expansion
        return 5;
    }
}
