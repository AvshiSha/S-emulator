package semulator.instructions;

import semulator.execution.ExecutionContext;
import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.variable.Variable;

import java.util.List;

public class JumpEqualFunctionInstruction extends AbstractInstruction {

    private final String functionName;
    private final List<Variable> functionArguments;
    private final Label target;

    public JumpEqualFunctionInstruction(Variable variable, String functionName, List<Variable> functionArguments, Label target) {
        super(InstructionData.JUMP_EQUAL_FUNCTION, variable);
        if (functionName == null || functionName.trim().isEmpty()) {
            throw new IllegalArgumentException("functionName cannot be null or empty");
        }
        if (functionArguments == null) {
            throw new IllegalArgumentException("functionArguments cannot be null");
        }
        if (target == null) {
            throw new IllegalArgumentException("target cannot be null");
        }
        this.functionName = functionName.trim();
        this.functionArguments = functionArguments;
        this.target = target;
    }

    public JumpEqualFunctionInstruction(Variable variable, String functionName, List<Variable> functionArguments, Label target, Label label) {
        super(InstructionData.JUMP_EQUAL_FUNCTION, variable, label);
        if (functionName == null || functionName.trim().isEmpty()) {
            throw new IllegalArgumentException("functionName cannot be null or empty");
        }
        if (functionArguments == null) {
            throw new IllegalArgumentException("functionArguments cannot be null");
        }
        if (target == null) {
            throw new IllegalArgumentException("target cannot be null");
        }
        this.functionName = functionName.trim();
        this.functionArguments = functionArguments;
        this.target = target;
    }

    @Override
    public Label execute(ExecutionContext context) {
        // The JUMP_EQUAL_FUNCTION instruction should not be executed directly.
        // Instead, it should be expanded during program loading/expansion phase.
        // This method should never be called in normal execution flow.
        throw new UnsupportedOperationException("JUMP_EQUAL_FUNCTION instruction should be expanded before execution");
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<Variable> getFunctionArguments() {
        return functionArguments;
    }

    public Label getTarget() {
        return target;
    }

    @Override
    public int cycles() {
        // JUMP_EQUAL_FUNCTION instructions have 6 base cycles plus the cycles of the quoted function
        // This will be calculated during expansion
        return 6;
    }
}
