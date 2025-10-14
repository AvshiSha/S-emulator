package com.semulator.engine.exec;

import com.semulator.engine.model.Variable;

import java.util.Map;

public interface ProgramExecutor {

    long run(Long... input);

    Map<Variable, Long> variableState();

    int getTotalCycles();
}
