package semulator.api;

import java.time.Instant;
import java.util.List;

/**
 * In-memory history snapshot for the façade lifetime (or until cleared).
 */
public interface History {
    List<Entry> entries();  // 1-based order for UI display

    default int size() {
        return entries().size();
    }

    record Entry(
            int index1Based,
            Instant at,
            String programName,
            int degree,
            List<Long> xInputs,
            long y,
            long totalCycles,
            int stepsExecuted
    ) {
    }
}
