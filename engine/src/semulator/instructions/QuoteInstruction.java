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
        // Execute the quoted function and assign its result to the target variable

        // For now, we'll implement a simple execution that calls the function
        // and assigns the result to the target variable

        // TODO: Implement proper function execution logic
        // This is a placeholder implementation
        // In a real implementation, we would:
        // 1. Get the function definition from the program
        // 2. Create a new execution context for the function
        // 3. Set up the function arguments
        // 4. Execute the function body
        // 5. Get the result and assign it to the target variable

        // For now, just assign 0 to the target variable as a placeholder
        // In debug mode, this will be handled by the debugger
        context.updateVariable(getVariable(), 0L);

        return FixedLabel.EMPTY;
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
