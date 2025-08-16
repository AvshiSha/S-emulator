package semulator.api;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * In-memory history snapshot for the façade lifetime (or until cleared).
 */
public interface History {
    List<Entry> entries();  // 1-based order for UI display

    default int size() {
        return entries().size();
    }

    /**
     * Immutable entry; replaces the previous 'record' Entry.
     */
    final class Entry {
        private final int index1Based;
        private final Instant at;
        private final String programName;
        private final int degree;
        private final List<Long> xInputs;
        private final long y;
        private final long totalCycles;
        private final int stepsExecuted;

        public Entry(int index1Based,
                     Instant at,
                     String programName,
                     int degree,
                     List<Long> xInputs,
                     long y,
                     long totalCycles,
                     int stepsExecuted) {
            if (index1Based < 1) throw new IllegalArgumentException("index1Based must be >= 1");
            this.index1Based = index1Based;
            this.at = Objects.requireNonNull(at, "at");
            this.programName = Objects.requireNonNull(programName, "programName");
            this.degree = degree;
            this.xInputs = List.copyOf(Objects.requireNonNull(xInputs, "xInputs"));
            this.y = y;
            this.totalCycles = totalCycles;
            this.stepsExecuted = stepsExecuted;
        }

        // record-style accessors (so call sites don't change)
        public int index1Based() {
            return index1Based;
        }

        public Instant at() {
            return at;
        }

        public String programName() {
            return programName;
        }

        public int degree() {
            return degree;
        }

        public List<Long> xInputs() {
            return xInputs;
        }

        public long y() {
            return y;
        }

        public long totalCycles() {
            return totalCycles;
        }

        public int stepsExecuted() {
            return stepsExecuted;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Entry that)) return false;
            return index1Based == that.index1Based &&
                    degree == that.degree &&
                    y == that.y &&
                    totalCycles == that.totalCycles &&
                    stepsExecuted == that.stepsExecuted &&
                    at.equals(that.at) &&
                    programName.equals(that.programName) &&
                    xInputs.equals(that.xInputs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(index1Based, at, programName, degree, xInputs, y, totalCycles, stepsExecuted);
        }

        @Override
        public String toString() {
            return "History.Entry[#%d at=%s program=%s degree=%d x=%s y=%d cycles=%d steps=%d]"
                    .formatted(index1Based, at, programName, degree, xInputs, y, totalCycles, stepsExecuted);
        }
    }
}
