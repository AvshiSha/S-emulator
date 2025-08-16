package semulator.execution;

import semulator.variable.Variable;

public interface ExecutionContext {
    long getVariableValue(Variable v);

    void updateVariable(Variable v, long value);
}
