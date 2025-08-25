package semulator.execution;

import semulator.variable.Variable;

import java.util.Map;

public interface ProgramExecutor {

    long run(Long... input);

    Map<Variable, Long> variableState();

    int getTotalCycles();
}