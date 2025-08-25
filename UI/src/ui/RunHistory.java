package ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages the history of program runs
 */
public class RunHistory {
    private final List<RunResult> runs = new ArrayList<>();
    private int nextRunNumber = 1;

    /**
     * Add a new run to the history
     */
    public void addRun(int level, List<Long> inputs, long yValue, int cycles) {
        RunResult run = new RunResult(nextRunNumber++, level, new ArrayList<>(inputs), yValue, cycles);
        runs.add(run);
    }

    /**
     * Get all runs in chronological order
     */
    public List<RunResult> getAllRuns() {
        return Collections.unmodifiableList(runs);
    }

    /**
     * Check if there are any runs in the history
     */
    public boolean isEmpty() {
        return runs.isEmpty();
    }

    /**
     * Get the number of runs in the history
     */
    public int size() {
        return runs.size();
    }

    /**
     * Clear all history
     */
    public void clear() {
        runs.clear();
        nextRunNumber = 1;
    }
}
