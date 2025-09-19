package semulator.instructions;

import semulator.execution.ExecutionContext;
import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.variable.Variable;

import java.util.List;

public class JumpEqualFunctionInstruction extends AbstractInstruction {

    private final String functionName;
    private final List<Variable> functionArguments;
    private final List<SInstruction> functionInstructions;
    private final Label target;

    public JumpEqualFunctionInstruction(Variable variable, String functionName, List<Variable> functionArguments,
            List<SInstruction> functionInstructions, Label target) {
        super(InstructionData.JUMP_EQUAL_FUNCTION, variable);
        if (functionName == null || functionName.trim().isEmpty()) {
            throw new IllegalArgumentException("functionName cannot be null or empty");
        }
        if (functionArguments == null) {
            throw new IllegalArgumentException("functionArguments cannot be null");
        }
        if (functionInstructions == null) {
            throw new IllegalArgumentException("functionInstructions cannot be null");
        }
        if (target == null) {
            throw new IllegalArgumentException("target cannot be null");
        }
        this.functionName = functionName.trim();
        this.functionArguments = functionArguments;
        this.functionInstructions = functionInstructions;
        this.target = target;
    }

    public JumpEqualFunctionInstruction(Variable variable, String functionName, List<Variable> functionArguments,
            List<SInstruction> functionInstructions, Label target, Label label) {
        super(InstructionData.JUMP_EQUAL_FUNCTION, variable, label);
        if (functionName == null || functionName.trim().isEmpty()) {
            throw new IllegalArgumentException("functionName cannot be null or empty");
        }
        if (functionArguments == null) {
            throw new IllegalArgumentException("functionArguments cannot be null");
        }
        if (functionInstructions == null) {
            throw new IllegalArgumentException("functionInstructions cannot be null");
        }
        if (target == null) {
            throw new IllegalArgumentException("target cannot be null");
        }
        this.functionName = functionName.trim();
        this.functionArguments = functionArguments;
        this.functionInstructions = functionInstructions;
        this.target = target;
    }

    @Override
    public Label execute(ExecutionContext context) {
        // Execute the function and compare its result with the variable
        // For now, we'll implement a simple execution that calls the function
        // and compares the result with the variable

        // Execute function and compare result with variable
        long functionResult = 0L; // Placeholder: function always returns 0
        long variableValue = context.getVariableValue(getVariable());

        if (variableValue == functionResult) {
            return target; // Jump if equal
        } else {
            return FixedLabel.EMPTY; // Continue if not equal
        }
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<Variable> getFunctionArguments() {
        return functionArguments;
    }

    public List<SInstruction> getFunctionInstructions() {
        return functionInstructions;
    }

    public Label getTarget() {
        return target;
    }

    @Override
    public int cycles() {
        // JUMP_EQUAL_FUNCTION instructions have 6 base cycles plus the cycles of the
        // quoted function
        int cycles = 0;
        for (SInstruction ins : functionInstructions) {
            cycles += ins.cycles();
        }
        return 6 + cycles;
    }
}
