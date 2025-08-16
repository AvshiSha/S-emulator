package semulator.api;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Executes the program with degree + inputs; returns full outcome including cycles and an (optionally)
 * expanded ProgramView that the UI should display for this run.
*/
public interface ProgramRunner {

    record RunOutcome(
            long y,
            LinkedHashMap<String, Long> usedVariablesOrdered, // y, x1.., z1.. (keys are textual names)
            long totalCycles,
            int stepsExecuted,
            ProgramView programViewShown,
            List<String> notes
    ) {
    }

    RunOutcome run(Object internalProgramModel, RunRequest request) throws SEmulatorException;
}
