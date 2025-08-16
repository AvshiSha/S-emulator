package semulator.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RunResult {
    private final long y;
    private final LinkedHashMap<String, Long> usedVariablesOrdered; // y, x1.., z1..
    private final long totalCycles;
    private final int stepsExecuted;
    private final ProgramView programViewShown;
    private final List<String> notes;

    public RunResult(long y,
                     LinkedHashMap<String, Long> usedVariablesOrdered,
                     long totalCycles,
                     int stepsExecuted,
                     ProgramView programViewShown,
                     List<String> notes) {
        this.y = y;
        // defensive copy to preserve insertion order & immutability outward
        this.usedVariablesOrdered = new LinkedHashMap<>(
                Objects.requireNonNull(usedVariablesOrdered, "usedVariablesOrdered"));
        this.totalCycles = totalCycles;
        this.stepsExecuted = stepsExecuted;
        this.programViewShown = Objects.requireNonNull(programViewShown, "programViewShown");
        this.notes = List.copyOf(Objects.requireNonNull(notes, "notes"));
    }

    // record-style accessors (return a defensive copy for the map)
    public long y() {
        return y;
    }

    public LinkedHashMap<String, Long> usedVariablesOrdered() {
        return new LinkedHashMap<>(usedVariablesOrdered);
    }

    public long totalCycles() {
        return totalCycles;
    }

    public int stepsExecuted() {
        return stepsExecuted;
    }

    public ProgramView programViewShown() {
        return programViewShown;
    }

    public List<String> notes() {
        return notes;
    }

    /**
     * Helper: reorder raw vars into the required order: y, x_i ascending, then z_i ascending.
     */
    public static LinkedHashMap<String, Long> orderedVars(Map<String, Long> vars) {
        return vars.entrySet().stream()
                .sorted((a, b) -> {
                    String ka = a.getKey(), kb = b.getKey();
                    if (ka.equals("y") && !kb.equals("y")) return -1;
                    if (kb.equals("y") && !ka.equals("y")) return 1;
                    String pa = prefix(ka), pb = prefix(kb);
                    if (!pa.equals(pb)) {
                        if (pa.equals("x")) return -1;
                        if (pb.equals("x")) return 1;
                        if (pa.equals("z")) return 1;   // x < y handled above; for others, z goes last
                        if (pb.equals("z")) return -1;
                    }
                    return suffix(ka) - suffix(kb);
                })
                .collect(LinkedHashMap::new,
                        (m, e) -> m.put(e.getKey(), e.getValue()),
                        LinkedHashMap::putAll);
    }

    private static String prefix(String s) {
        return s.substring(0, 1);
    }

    private static int suffix(String s) {
        return s.equals("y") ? 0 : Integer.parseInt(s.substring(1));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RunResult that)) return false;
        return y == that.y &&
                totalCycles == that.totalCycles &&
                stepsExecuted == that.stepsExecuted &&
                usedVariablesOrdered.equals(that.usedVariablesOrdered) &&
                programViewShown.equals(that.programViewShown) &&
                notes.equals(that.notes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(y, usedVariablesOrdered, totalCycles, stepsExecuted, programViewShown, notes);
    }

    @Override
    public String toString() {
        return "RunResult[y=%d, totalCycles=%d, steps=%d, vars=%s]"
                .formatted(y, totalCycles, stepsExecuted, usedVariablesOrdered);
    }
}
