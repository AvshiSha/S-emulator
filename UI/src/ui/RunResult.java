package ui;

import java.util.List;

/**
 * Record to store the results of a program run
 */
public record RunResult(
        int runNumber,
        int level,
        List<Long> inputs,
        long yValue,
        int cycles) {
    /**
     * Convert inputs to CSV format for display
     */
    public String inputsCsv() {
        if (inputs == null || inputs.isEmpty()) {
            return "";
        }
        return inputs.stream()
                .map(String::valueOf)
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }
}
