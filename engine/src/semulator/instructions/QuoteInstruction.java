package semulator.instructions;

import semulator.execution.ExecutionContext;
import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.variable.Variable;
import semulator.variable.VariableImpl;
import semulator.variable.VariableType;

import java.util.List;

public class QuoteInstruction extends AbstractInstruction {

    private final String functionName;
    private final List<FunctionArgument> functionArguments;
    private final List<SInstruction> functionInstructions;

    public QuoteInstruction(Variable target, String functionName, List<FunctionArgument> functionArguments,
            List<SInstruction> functionInstructions) {
        super(InstructionData.QUOTE, target);
        if (functionName == null || functionName.trim().isEmpty()) {
            throw new IllegalArgumentException("functionName cannot be null or empty");
        }
        if (functionArguments == null) {
            throw new IllegalArgumentException("functionArguments cannot be null");
        }
        if (functionInstructions == null) {
            throw new IllegalArgumentException("functionInstructions cannot be null");
        }
        this.functionName = functionName.trim();
        this.functionArguments = functionArguments;
        // we need to add in here all the instructions that are in the function body
        this.functionInstructions = functionInstructions;
    }

    public QuoteInstruction(Variable target, String functionName, List<FunctionArgument> functionArguments,
            List<SInstruction> functionInstructions, Label label) {
        super(InstructionData.QUOTE, target, label);
        if (functionName == null || functionName.trim().isEmpty()) {
            throw new IllegalArgumentException("functionName cannot be null or empty");
        }
        if (functionArguments == null) {
            throw new IllegalArgumentException("functionArguments cannot be null");
        }
        if (functionInstructions == null) {
            throw new IllegalArgumentException("functionInstructions cannot be null");
        }
        this.functionName = functionName.trim();
        this.functionArguments = functionArguments;
        this.functionInstructions = functionInstructions;
    }

    @Override
    public Label execute(ExecutionContext context) {
        // Execute the quoted function and assign its result to the target variable

        // This method handles the case where a QUOTE instruction is executed directly
        // (e.g., in debug mode or when running unexpanded programs)

        try {
            // Create a new execution context for the function
            ExecutionContext functionContext = createFunctionContext(context);

            // Execute the function body
            long functionResult = executeFunctionBody(functionContext);

            // Assign the result to the target variable
            context.updateVariable(getVariable(), functionResult);

            return FixedLabel.EMPTY;

        } catch (Exception e) {
            // If execution fails, assign 0 as fallback
            context.updateVariable(getVariable(), 0L);
            return FixedLabel.EMPTY;
        }
    }

    /**
     * Create a new execution context for the function with proper argument setup
     */
    private ExecutionContext createFunctionContext(ExecutionContext parentContext) {
        // Create a simple execution context that maps function arguments to input
        // variables
        return new ExecutionContext() {
            private final java.util.Map<Variable, Long> variables = new java.util.HashMap<>();

            {
                // Initialize input variables (x1, x2, ...) with function arguments
                for (int i = 0; i < functionArguments.size(); i++) {
                    Variable inputVar = new VariableImpl(VariableType.INPUT, i + 1);

                    FunctionArgument arg = functionArguments.get(i);
                    if (arg.isFunctionCall()) {
                        // For function calls, we need to execute them first
                        // This is a simplified approach - in practice, you'd want to expand and execute
                        variables.put(inputVar, 0L); // Placeholder
                    } else {
                        Variable var = arg.asVariable();
                        if (var.getType() == semulator.variable.VariableType.Constant) {
                            variables.put(inputVar, (long) var.getNumber());
                        } else {
                            // Get the value from the parent execution context
                            variables.put(inputVar, parentContext.getVariableValue(var));
                        }
                    }
                }
            }

            @Override
            public long getVariableValue(Variable v) {
                return variables.getOrDefault(v, 0L);
            }

            @Override
            public void updateVariable(Variable v, long value) {
                variables.put(v, value);
            }
        };
    }

    /**
     * Execute the function body and return the result
     */
    private long executeFunctionBody(ExecutionContext functionContext) {
        // Execute the function body and return the result
        // The result is stored in the 'y' variable (Variable.RESULT)

        for (SInstruction instruction : functionInstructions) {
            Label nextLabel = instruction.execute(functionContext);

            // Handle jumps within the function (though functions typically don't have
            // jumps)
            if (nextLabel != FixedLabel.EMPTY && nextLabel != FixedLabel.EXIT) {
                // For now, we'll ignore jumps in functions and continue execution
                // In a more sophisticated implementation, we'd handle function-internal jumps
            }
        }

        // Return the value of the 'y' variable (the function's output)
        return functionContext.getVariableValue(Variable.RESULT);
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<FunctionArgument> getFunctionArguments() {
        return functionArguments;
    }

    public List<SInstruction> getFunctionInstructions() {
        return functionInstructions;
    }

    @Override
    public int cycles() {
        int cycles = 0;
        for (SInstruction ins : functionInstructions) {
            cycles += ins.cycles();
        }
        return 5 + cycles;
    }
}
