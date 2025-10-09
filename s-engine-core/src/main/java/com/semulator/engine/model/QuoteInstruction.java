package com.semulator.engine.model;

import com.semulator.engine.exec.ExecutionContext;

import java.util.List;
import java.util.Map;

public class QuoteInstruction extends AbstractInstruction {

    private final String functionName;
    private final List<FunctionArgument> functionArguments;
    private final List<SInstruction> functionInstructions;
    private final Map<String, List<SInstruction>> functions;

    public QuoteInstruction(Variable target, String functionName, List<FunctionArgument> functionArguments,
            List<SInstruction> functionInstructions, Map<String, List<SInstruction>> functions) {
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
        this.functions = functions;
    }

    public QuoteInstruction(Variable target, String functionName, List<FunctionArgument> functionArguments,
            List<SInstruction> functionInstructions, Label label, Map<String, List<SInstruction>> functions) {
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
        this.functions = functions;
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
                        FunctionCall call = arg.asFunctionCall();
                        long nestedResult = executeNestedFunctionCall(call, parentContext);
                        variables.put(inputVar, nestedResult);
                    } else {
                        Variable var = arg.asVariable();
                        if (var.getType() == VariableType.Constant) {
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

        int instructionIndex = 0;
        while (instructionIndex < functionInstructions.size()) {
            SInstruction instruction = functionInstructions.get(instructionIndex);
            Label nextLabel;

            nextLabel = instruction.execute(functionContext);

            // Handle jumps within the function
            if (nextLabel == FixedLabel.EXIT) {
                break; // Exit the function
            } else if (nextLabel != FixedLabel.EMPTY) {
                // Find the target instruction by label
                int targetIndex = findInstructionByLabel(nextLabel);
                if (targetIndex != -1) {
                    instructionIndex = targetIndex;
                } else {
                    // Label not found, continue to next instruction
                    instructionIndex++;
                }
            } else {
                // No jump, continue to next instruction
                instructionIndex++;
            }
        }

        // Return the value of the 'y' variable (the function's output)
        return functionContext.getVariableValue(Variable.RESULT);
    }

    /**
     * Execute a nested function call and return its result
     */
    private long executeNestedFunctionCall(FunctionCall call, ExecutionContext parentContext) {
        if (functions == null) {
            throw new IllegalStateException("Functions map not available for nested function execution");
        }

        // Get the function body for the nested function
        List<SInstruction> nestedFunctionBody = functions.get(call.getFunctionName());
        if (nestedFunctionBody == null) {
            throw new IllegalArgumentException("Function '" + call.getFunctionName() + "' not found");
        }

        // Create execution context for the nested function
        ExecutionContext nestedContext = createNestedFunctionContext(call, parentContext);

        // Execute the nested function body
        return executeFunctionBody(nestedContext, nestedFunctionBody);
    }

    /**
     * Create execution context for a nested function call
     */
    private ExecutionContext createNestedFunctionContext(FunctionCall call, ExecutionContext parentContext) {
        return new ExecutionContext() {
            private final java.util.Map<Variable, Long> variables = new java.util.HashMap<>();

            {
                // Initialize input variables (x1, x2, ...) with nested function arguments
                for (int i = 0; i < call.getArguments().size(); i++) {
                    Variable inputVar = new VariableImpl(VariableType.INPUT, i + 1);
                    FunctionArgument arg = call.getArguments().get(i);

                    if (arg.isFunctionCall()) {
                        // Recursively execute nested function calls
                        FunctionCall nestedCall = arg.asFunctionCall();
                        long nestedResult = executeNestedFunctionCall(nestedCall, parentContext);
                        variables.put(inputVar, nestedResult);
                    } else {
                        Variable var = arg.asVariable();
                        if (var.getType() == VariableType.Constant) {
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
     * Execute function body with given context and instructions
     */
    private long executeFunctionBody(ExecutionContext functionContext, List<SInstruction> instructions) {
        int instructionIndex = 0;
        while (instructionIndex < instructions.size()) {
            SInstruction instruction = instructions.get(instructionIndex);
            Label nextLabel = instruction.execute(functionContext);

            // Handle jumps within the function
            if (nextLabel == FixedLabel.EXIT) {
                break; // Exit the function
            } else if (nextLabel != FixedLabel.EMPTY) {
                // Find the target instruction by label
                int targetIndex = findInstructionByLabel(nextLabel, instructions);
                if (targetIndex != -1) {
                    instructionIndex = targetIndex;
                } else {
                    // Label not found, continue to next instruction
                    instructionIndex++;
                }
            } else {
                // No jump, continue to next instruction
                instructionIndex++;
            }
        }

        // Return the value of the 'y' variable (the function's output)
        return functionContext.getVariableValue(Variable.RESULT);
    }

    /**
     * Find the index of an instruction with the given label
     */
    private int findInstructionByLabel(Label targetLabel) {
        return findInstructionByLabel(targetLabel, functionInstructions);
    }

    /**
     * Find the index of an instruction with the given label in a specific
     * instruction list
     */
    private int findInstructionByLabel(Label targetLabel, List<SInstruction> instructions) {
        for (int i = 0; i < instructions.size(); i++) {
            SInstruction instruction = instructions.get(i);
            if (instruction.getLabel() != null && instruction.getLabel().equals(targetLabel)) {
                return i;
            }
        }
        return -1; // Label not found
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
