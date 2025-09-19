package semulator.execution;

import semulator.instructions.SInstruction;
import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.program.SProgram;
import semulator.variable.Variable;
import semulator.variable.VariableImpl;
import semulator.variable.VariableType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProgramExecutorImpl implements ProgramExecutor {

    private final SProgram program;
    private int totalCycles = 0;
    private ExecutionContext lastContext = null;

    public ProgramExecutorImpl(SProgram program) {
        this.program = program;
    }

    @Override
    public long run(Long... input) {
        // Reset cycle counter
        totalCycles = 0;

        // Create execution context with proper input initialization
        ExecutionContext context = new LocalExecutionContext(input);
        lastContext = context;

        // Use the original program instructions (don't expand synthetic instructions)
        List<SInstruction> instructions = program.getInstructions();

        // Build label-to-instruction map for efficient jumping
        Map<String, Integer> labelToIndex = buildLabelMap(instructions);

        // Start with the first instruction
        int currentIndex = 0;

        while (currentIndex < instructions.size()) {
            SInstruction currentInstruction = instructions.get(currentIndex);
            totalCycles += currentInstruction.cycles();

            // Handle QUOTE and JUMP_EQUAL_FUNCTION instructions specially
            Label nextLabel;
            if (currentInstruction instanceof semulator.instructions.QuoteInstruction quoteInstruction) {
                nextLabel = executeQuoteInstruction(quoteInstruction, context);
            } else if (currentInstruction instanceof semulator.instructions.JumpEqualFunctionInstruction jumpEqualFunctionInstruction) {
                nextLabel = executeJumpEqualFunctionInstruction(jumpEqualFunctionInstruction, context);
            } else {
                // Execute the instruction and get the next label
                nextLabel = currentInstruction.execute(context);
            }

            // Determine next instruction based on the returned label
            if (nextLabel == FixedLabel.EMPTY) {
                // Continue to next instruction in sequence
                currentIndex++;
            } else if (nextLabel == FixedLabel.EXIT) {
                // Exit the program
                break;
            } else {
                // Jump to the label
                String labelName = nextLabel.getLabel();
                Integer targetIndex = labelToIndex.get(labelName);
                if (targetIndex != null) {
                    currentIndex = targetIndex;
                } else {
                    // If label not found, continue to next instruction
                    currentIndex++;
                }
            }
        }

        return context.getVariableValue(Variable.RESULT);
    }

    private Map<String, Integer> buildLabelMap(List<SInstruction> instructions) {
        Map<String, Integer> labelMap = new HashMap<>();

        // First pass: collect all labels that are actually defined on instructions
        for (int i = 0; i < instructions.size(); i++) {
            SInstruction instruction = instructions.get(i);
            Label label = instruction.getLabel();

            if (label != null && label != FixedLabel.EMPTY && label != FixedLabel.EXIT) {
                String labelName = label.getLabel();
                if (labelName != null && !labelName.isEmpty()) {
                    labelMap.put(labelName, i);
                }
            }
        }

        // Second pass: validate that all jump targets exist in our label map
        for (SInstruction instruction : instructions) {
            Label target = null;

            if (instruction instanceof semulator.instructions.JumpNotZeroInstruction jnz) {
                target = jnz.getTarget();
            } else if (instruction instanceof semulator.instructions.JumpZeroInstruction jz) {
                target = jz.getTarget();
            } else if (instruction instanceof semulator.instructions.JumpEqualConstantInstruction jec) {
                target = jec.getTarget();
            } else if (instruction instanceof semulator.instructions.JumpEqualVariableInstruction jev) {
                target = jev.getTarget();
            } else if (instruction instanceof semulator.instructions.GotoLabelInstruction gotoInst) {
                target = gotoInst.getTarget();
            }

            if (target != null && target != FixedLabel.EMPTY && target != FixedLabel.EXIT) {
                String targetName = target.getLabel();
                if (targetName != null && !targetName.isEmpty() && !labelMap.containsKey(targetName)) {
                    // Jump target validation - could be extended for error reporting
                }
            }
        }

        return labelMap;
    }

    @Override
    public Map<Variable, Long> variableState() {
        if (lastContext instanceof LocalExecutionContext) {
            return ((LocalExecutionContext) lastContext).getAllVariables();
        }
        return Map.of();
    }

    public int getTotalCycles() {
        return totalCycles;
    }

    // Simple in-memory execution context that defaults variables to 0L.
    private static final class LocalExecutionContext implements ExecutionContext {
        private final Map<Variable, Long> state = new HashMap<>();

        LocalExecutionContext(Long... input) {
            if (input != null) {
                for (int i = 0; i < input.length; i++) {
                    Variable inputVar = new VariableImpl(VariableType.INPUT, i + 1);
                    state.put(inputVar, input[i] == null ? 0L : input[i]);
                }
            }
        }

        @Override
        public long getVariableValue(Variable v) {
            return state.getOrDefault(v, 0L);
        }

        @Override
        public void updateVariable(Variable v, long value) {
            state.put(v, value);
        }

        public Map<Variable, Long> getAllVariables() {
            return new HashMap<>(state);
        }
    }

    private Label executeQuoteInstruction(semulator.instructions.QuoteInstruction quoteInstruction,
            ExecutionContext context) {
        try {
            // Get the function definition from the program
            if (!(program instanceof semulator.program.SProgramImpl)) {
                // If we can't get the function, just assign 0 as fallback
                context.updateVariable(quoteInstruction.getVariable(), 0L);
                return FixedLabel.EMPTY;
            }

            semulator.program.SProgramImpl programImpl = (semulator.program.SProgramImpl) program;
            var functions = programImpl.getFunctions();
            String functionName = quoteInstruction.getFunctionName();

            if (!functions.containsKey(functionName)) {
                // Function not found, assign 0 as fallback
                context.updateVariable(quoteInstruction.getVariable(), 0L);
                return FixedLabel.EMPTY;
            }

            // Get the function body
            var functionInstructions = functions.get(functionName);

            // Create a new execution context for the function
            java.util.List<Long> functionInputs = new java.util.ArrayList<>();
            for (semulator.instructions.FunctionArgument arg : quoteInstruction.getFunctionArguments()) {
                if (arg.isFunctionCall()) {
                    // For function calls, we need to execute them first
                    // This is a simplified approach - in practice, you'd want to expand and execute
                    functionInputs.add(0L); // Placeholder - function calls should be expanded before execution
                } else {
                    semulator.variable.Variable var = arg.asVariable();
                    if (var.getType() == semulator.variable.VariableType.Constant) {
                        functionInputs.add((long) var.getNumber());
                    } else {
                        // Get the value from the current execution context
                        functionInputs.add(context.getVariableValue(var));
                    }
                }
            }

            LocalExecutionContext functionContext = new LocalExecutionContext(functionInputs.toArray(new Long[0]));

            // Execute the function body
            long functionResult = executeFunctionBody(functionInstructions, functionContext);

            // Assign the result to the target variable
            context.updateVariable(quoteInstruction.getVariable(), functionResult);

            return FixedLabel.EMPTY;

        } catch (Exception e) {
            System.err.println("Error executing quote instruction: " + e.getMessage());
            // Fallback: assign 0 to the target variable
            context.updateVariable(quoteInstruction.getVariable(), 0L);
            return FixedLabel.EMPTY;
        }
    }

    private Label executeJumpEqualFunctionInstruction(
            semulator.instructions.JumpEqualFunctionInstruction jumpEqualFunctionInstruction,
            ExecutionContext context) {
        try {
            // Execute the function and compare its result with the variable
            long functionResult = executeFunctionForJumpEqual(jumpEqualFunctionInstruction, context);
            long variableValue = context.getVariableValue(jumpEqualFunctionInstruction.getVariable());

            if (variableValue == functionResult) {
                return jumpEqualFunctionInstruction.getTarget(); // Jump if equal
            } else {
                return FixedLabel.EMPTY; // Continue if not equal
            }

        } catch (Exception e) {
            System.err.println("Error executing jump equal function instruction: " + e.getMessage());
            // Fallback: don't jump
            return FixedLabel.EMPTY;
        }
    }

    private long executeFunctionForJumpEqual(
            semulator.instructions.JumpEqualFunctionInstruction jumpEqualFunctionInstruction,
            ExecutionContext context) {
        try {
            // Get the function definition from the program
            if (!(program instanceof semulator.program.SProgramImpl)) {
                return 0L; // Fallback
            }

            semulator.program.SProgramImpl programImpl = (semulator.program.SProgramImpl) program;
            var functions = programImpl.getFunctions();
            String functionName = jumpEqualFunctionInstruction.getFunctionName();

            if (!functions.containsKey(functionName)) {
                return 0L; // Fallback
            }

            // Get the function body
            var functionInstructions = functions.get(functionName);

            // Create a new execution context for the function
            java.util.List<Long> functionInputs = new java.util.ArrayList<>();
            for (semulator.instructions.FunctionArgument arg : jumpEqualFunctionInstruction.getFunctionArguments()) {
                if (arg.isFunctionCall()) {
                    // For function calls, we need to execute them first
                    functionInputs.add(0L); // Placeholder - function calls should be expanded before execution
                } else {
                    semulator.variable.Variable var = arg.asVariable();
                    if (var.getType() == semulator.variable.VariableType.Constant) {
                        functionInputs.add((long) var.getNumber());
                    } else {
                        // Get the value from the current execution context
                        functionInputs.add(context.getVariableValue(var));
                    }
                }
            }

            LocalExecutionContext functionContext = new LocalExecutionContext(functionInputs.toArray(new Long[0]));

            // Execute the function body and return the result
            return executeFunctionBody(functionInstructions, functionContext);

        } catch (Exception e) {
            System.err.println("Error executing function for jump equal: " + e.getMessage());
            return 0L; // Fallback
        }
    }

    private long executeFunctionBody(java.util.List<semulator.instructions.SInstruction> functionInstructions,
            LocalExecutionContext functionContext) {
        // Execute the function body and return the result
        // The result is stored in the 'y' variable (Variable.RESULT)

        for (semulator.instructions.SInstruction instruction : functionInstructions) {
            semulator.label.Label nextLabel = instruction.execute(functionContext);

            // Handle jumps within the function (though functions typically don't have
            // jumps)
            if (nextLabel != semulator.label.FixedLabel.EMPTY && nextLabel != semulator.label.FixedLabel.EXIT) {
                // For now, we'll ignore jumps in functions and continue execution
                // In a more sophisticated implementation, we'd handle function-internal jumps
            }
        }

        // Return the value of the 'y' variable (the function's output)
        return functionContext.getVariableValue(semulator.variable.Variable.RESULT);
    }
}
