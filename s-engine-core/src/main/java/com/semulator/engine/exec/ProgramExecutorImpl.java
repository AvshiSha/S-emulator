package com.semulator.engine.exec;

import com.semulator.engine.model.SInstruction;
import com.semulator.engine.model.FixedLabel;
import com.semulator.engine.model.Label;
import com.semulator.engine.model.SProgram;
import com.semulator.engine.model.Variable;
import com.semulator.engine.model.VariableImpl;
import com.semulator.engine.model.VariableType;

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
        int count = 0;
        while (currentIndex < instructions.size()) {
            SInstruction currentInstruction = instructions.get(currentIndex);
            totalCycles += currentInstruction.cycles();
            count++;
            // Print instruction execution to console with readable format
            String instructionText = formatInstruction(currentInstruction);

            // Handle QUOTE and JUMP_EQUAL_FUNCTION instructions specially
            Label nextLabel;
            if (currentInstruction instanceof com.semulator.engine.model.QuoteInstruction quoteInstruction) {
                nextLabel = executeQuoteInstruction(quoteInstruction, context);
            } else if (currentInstruction instanceof com.semulator.engine.model.JumpEqualFunctionInstruction jumpEqualFunctionInstruction) {
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

            // // // Safety check to prevent infinite loops
            // if (totalCycles > 10000) {
            // break;
            // }
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

            if (instruction instanceof com.semulator.engine.model.JumpNotZeroInstruction jnz) {
                target = jnz.getTarget();
            } else if (instruction instanceof com.semulator.engine.model.JumpZeroInstruction jz) {
                target = jz.getTarget();
            } else if (instruction instanceof com.semulator.engine.model.JumpEqualConstantInstruction jec) {
                target = jec.getTarget();
            } else if (instruction instanceof com.semulator.engine.model.JumpEqualVariableInstruction jev) {
                target = jev.getTarget();
            } else if (instruction instanceof com.semulator.engine.model.GotoLabelInstruction gotoInst) {
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

    private Label executeQuoteInstruction(com.semulator.engine.model.QuoteInstruction quoteInstruction,
            ExecutionContext context) {
        try {
            // Get the function definition from the program
            if (!(program instanceof com.semulator.engine.parse.SProgramImpl)) {
                // If we can't get the function, just assign 0 as fallback
                context.updateVariable(quoteInstruction.getVariable(), 0L);
                return FixedLabel.EMPTY;
            }

            com.semulator.engine.parse.SProgramImpl programImpl = (com.semulator.engine.parse.SProgramImpl) program;
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
            try {
                for (com.semulator.engine.model.FunctionArgument arg : quoteInstruction.getFunctionArguments()) {
                    if (arg.isFunctionCall()) {
                        // For function calls, we need to execute them first
                        com.semulator.engine.model.FunctionCall call = arg.asFunctionCall();
                        try {
                            long nestedResult = executeNestedFunctionCall(call, context, functions);
                            functionInputs.add(nestedResult);
                        } catch (Exception e) {
                            // Error during execution
                            functionInputs.add(0L);
                        }
                    } else {
                        com.semulator.engine.model.Variable var = arg.asVariable();
                        if (var.getType() == com.semulator.engine.model.VariableType.Constant) {
                            functionInputs.add((long) var.getNumber());
                        } else {
                            // Get the value from the current execution context
                            functionInputs.add(context.getVariableValue(var));
                        }
                    }
                }
            } catch (Exception e) {
                // Error during execution
                throw e;
            }

            LocalExecutionContext functionContext = new LocalExecutionContext(functionInputs.toArray(new Long[0]));

            // Execute the function body
            long functionResult = executeFunctionBody(functionInstructions, functionContext);

            // Assign the result to the target variable
            context.updateVariable(quoteInstruction.getVariable(), functionResult);

            return FixedLabel.EMPTY;

        } catch (Exception e) {
            // Error during execution
            // Fallback: assign 0 to the target variable
            context.updateVariable(quoteInstruction.getVariable(), 0L);
            return FixedLabel.EMPTY;
        }
    }

    /**
     * Execute a nested function call and return its result
     */
    private long executeNestedFunctionCall(com.semulator.engine.model.FunctionCall call, ExecutionContext parentContext,
            java.util.Map<String, java.util.List<com.semulator.engine.model.SInstruction>> functions) {

        // Get the function body for the nested function
        java.util.List<com.semulator.engine.model.SInstruction> nestedFunctionBody = functions
                .get(call.getFunctionName());
        if (nestedFunctionBody == null) {
            throw new IllegalArgumentException("Function '" + call.getFunctionName() + "' not found");
        }

        // Create execution context for the nested function
        java.util.List<Long> nestedInputs = new java.util.ArrayList<>();
        for (com.semulator.engine.model.FunctionArgument arg : call.getArguments()) {
            if (arg.isFunctionCall()) {
                // Recursively execute nested function calls
                com.semulator.engine.model.FunctionCall nestedCall = arg.asFunctionCall();
                long nestedResult = executeNestedFunctionCall(nestedCall, parentContext, functions);
                nestedInputs.add(nestedResult);
            } else {
                com.semulator.engine.model.Variable var = arg.asVariable();
                if (var.getType() == com.semulator.engine.model.VariableType.Constant) {
                    nestedInputs.add((long) var.getNumber());
                } else {
                    // Get the value from the parent execution context
                    long varValue = parentContext.getVariableValue(var);
                    nestedInputs.add(varValue);
                }
            }
        }

        LocalExecutionContext nestedContext = new LocalExecutionContext(nestedInputs.toArray(new Long[0]));

        // Execute the nested function body
        long result = executeFunctionBody(nestedFunctionBody, nestedContext);
        return result;
    }

    private Label executeJumpEqualFunctionInstruction(
            com.semulator.engine.model.JumpEqualFunctionInstruction jumpEqualFunctionInstruction,
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
            // Error executing jump equal function instruction
            // Fallback: don't jump
            return FixedLabel.EMPTY;
        }
    }

    private long executeFunctionForJumpEqual(
            com.semulator.engine.model.JumpEqualFunctionInstruction jumpEqualFunctionInstruction,
            ExecutionContext context) {
        try {
            // Get the function definition from the program
            if (!(program instanceof com.semulator.engine.parse.SProgramImpl)) {
                return 0L; // Fallback
            }

            com.semulator.engine.parse.SProgramImpl programImpl = (com.semulator.engine.parse.SProgramImpl) program;
            var functions = programImpl.getFunctions();
            String functionName = jumpEqualFunctionInstruction.getFunctionName();

            if (!functions.containsKey(functionName)) {
                return 0L; // Fallback
            }

            // Get the function body
            var functionInstructions = functions.get(functionName);

            // Create a new execution context for the function
            java.util.List<Long> functionInputs = new java.util.ArrayList<>();
            for (com.semulator.engine.model.FunctionArgument arg : jumpEqualFunctionInstruction
                    .getFunctionArguments()) {
                if (arg.isFunctionCall()) {
                    // For function calls, we need to execute them first
                    functionInputs.add(0L); // Placeholder - function calls should be expanded before execution
                } else {
                    com.semulator.engine.model.Variable var = arg.asVariable();
                    if (var.getType() == com.semulator.engine.model.VariableType.Constant) {
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
            // Error executing function for jump equal
            return 0L; // Fallback
        }
    }

    private long executeFunctionBody(java.util.List<com.semulator.engine.model.SInstruction> functionInstructions,
            LocalExecutionContext functionContext) {
        // Execute the function body and return the result
        // The result is stored in the 'y' variable (Variable.RESULT)

        // Build label-to-instruction map for efficient jumping within the function
        Map<String, Integer> labelToIndex = buildLabelMap(functionInstructions);

        int instructionIndex = 0;
        while (instructionIndex < functionInstructions.size()) {
            com.semulator.engine.model.SInstruction instruction = functionInstructions.get(instructionIndex);
            com.semulator.engine.model.Label nextLabel = instruction.execute(functionContext);

            // Handle jumps within the function
            if (nextLabel == com.semulator.engine.model.FixedLabel.EXIT) {
                break; // Exit the function
            } else if (nextLabel != com.semulator.engine.model.FixedLabel.EMPTY) {
                // Find the target instruction by label
                String labelName = nextLabel.getLabel();
                if ("EXIT".equals(labelName)) {
                    break; // Exit the function
                }
                Integer targetIndex = labelToIndex.get(labelName);
                if (targetIndex != null) {
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
        return functionContext.getVariableValue(com.semulator.engine.model.Variable.RESULT);
    }

    /**
     * Format an instruction for readable console output
     */
    private String formatInstruction(SInstruction instruction) {
        if (instruction instanceof com.semulator.engine.model.IncreaseInstruction) {
            return instruction.getVariable() + " <- " + instruction.getVariable() + " + 1";
        } else if (instruction instanceof com.semulator.engine.model.DecreaseInstruction) {
            return instruction.getVariable() + " <- " + instruction.getVariable() + " - 1";
        } else if (instruction instanceof com.semulator.engine.model.NoOpInstruction) {
            return instruction.getVariable() + " <- " + instruction.getVariable();
        } else if (instruction instanceof com.semulator.engine.model.ZeroVariableInstruction) {
            return instruction.getVariable() + " <- 0";
        } else if (instruction instanceof com.semulator.engine.model.AssignVariableInstruction a) {
            return instruction.getVariable() + " <- " + a.getSource();
        } else if (instruction instanceof com.semulator.engine.model.AssignConstantInstruction c) {
            return instruction.getVariable() + " <- " + c.getConstant();
        } else if (instruction instanceof com.semulator.engine.model.GotoLabelInstruction g) {
            return "GOTO " + g.getTarget();
        } else if (instruction instanceof com.semulator.engine.model.JumpNotZeroInstruction j) {
            return "IF " + j.getVariable() + " != 0 GOTO " + j.getTarget();
        } else if (instruction instanceof com.semulator.engine.model.JumpZeroInstruction j) {
            return "IF " + j.getVariable() + " == 0 GOTO " + j.getTarget();
        } else if (instruction instanceof com.semulator.engine.model.JumpEqualConstantInstruction j) {
            return "IF " + j.getVariable() + " == " + j.getConstant() + " GOTO " + j.getTarget();
        } else if (instruction instanceof com.semulator.engine.model.JumpEqualVariableInstruction j) {
            return "IF " + j.getVariable() + " == " + j.getOther() + " GOTO " + j.getTarget();
        } else if (instruction instanceof com.semulator.engine.model.QuoteInstruction q) {
            String arguments = "";
            java.util.List<com.semulator.engine.model.FunctionArgument> args = q.getFunctionArguments();
            for (int i = 0; i < args.size(); i++) {
                if (i > 0)
                    arguments += ",";
                arguments += args.get(i).toString();
            }
            return instruction.getVariable() + " <- (" + q.getFunctionName() + "," + arguments + ")";
        } else if (instruction instanceof com.semulator.engine.model.JumpEqualFunctionInstruction j) {
            String arguments = "";
            java.util.List<com.semulator.engine.model.FunctionArgument> args = j.getFunctionArguments();
            for (int i = 0; i < args.size(); i++) {
                if (i > 0)
                    arguments += ",";
                arguments += args.get(i).toString();
            }
            return "IF " + j.getVariable() + " == (" + j.getFunctionName() + "," + arguments + ") GOTO "
                    + j.getTarget();
        } else {
            return instruction.getClass().getSimpleName() + " " + instruction.toString();
        }
    }
}
