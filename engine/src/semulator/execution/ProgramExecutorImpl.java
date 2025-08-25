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

        // Build label-to-instruction map for efficient jumping
        Map<String, Integer> labelToIndex = buildLabelMap();

        // Start with the first instruction
        int currentIndex = 0;
        List<SInstruction> instructions = program.getInstructions();

        while (currentIndex < instructions.size()) {
            SInstruction currentInstruction = instructions.get(currentIndex);

            // Execute the instruction and get the next label
            Label nextLabel = currentInstruction.execute(context);

            // Add cycles for this instruction
            totalCycles += currentInstruction.cycles();

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

    private Map<String, Integer> buildLabelMap() {
        Map<String, Integer> labelMap = new HashMap<>();
        List<SInstruction> instructions = program.getInstructions();

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
        // (This is for debugging - we don't add them here since they should already be
        // in the map)
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
                    // This is a warning - the jump target doesn't exist in our label map
                    // This could happen if there's a bug in the program or expansion
                    // System.err.println("Warning: Jump target '" + targetName + "' not found in
                    // label map");
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
            // Initialize all variables to 0 by default
            // Variables will be initialized when first accessed via getVariableValue

            // Initialize input variables if provided
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
}
