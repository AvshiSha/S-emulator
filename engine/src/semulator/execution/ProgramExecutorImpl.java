package semulator.execution;

import semulator.instructions.SInstruction;
import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.program.SProgram;
import semulator.variable.Variable;

import java.util.HashMap;
import java.util.Map;

public class ProgramExecutorImpl implements ProgramExecutor {

    private final SProgram program;

    public ProgramExecutorImpl(SProgram program) {
        this.program = program;
    }

    @Override
    public long run(Long... input) {

        ExecutionContext context = new LocalExecutionContext(input);

        SInstruction currentInstruction = program.getInstructions().getFirst();
        Label nextLabel;
        do {
            nextLabel = currentInstruction.execute(context);

            if (nextLabel == FixedLabel.EMPTY) {
                // set currentInstruction to the next instruction in line
            } else if (nextLabel != FixedLabel.EXIT) {
                // need to find the instruction at 'nextLabel' and set current instruction to it
            }
        } while (nextLabel != FixedLabel.EXIT);

        return context.getVariableValue(Variable.RESULT);
    }


    @Override
    public Map<Variable, Long> variableState() {
        return Map.of();
    }

    // Simple in-memory execution context that defaults variables to 0L.
    private static final class LocalExecutionContext implements ExecutionContext {
        private final Map<Variable, Long> state = new HashMap<>();

        LocalExecutionContext(Long... input) {
            // If you need to seed specific input variables, map them here.
            // As a safe default, initialize RESULT to the first input if provided; otherwise 0.
            if (input != null && input.length > 0) {
                state.put(Variable.RESULT, input[0] == null ? 0L : input[0]);
            } else {
                state.put(Variable.RESULT, 0L);
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
    }
}
