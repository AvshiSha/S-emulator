package com.semulator.engine.exec;

import com.semulator.engine.model.Variable;

public interface ExecutionContext {
    long getVariableValue(Variable v);

    void updateVariable(Variable v, long value);
}
