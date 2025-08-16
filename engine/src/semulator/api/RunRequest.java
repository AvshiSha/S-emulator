package semulator.api;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class RunRequest {
    private final int degree;
    private final List<Long> xInputs;

    public RunRequest(int degree, List<Long> xInputs) {
        if (degree < 0) throw new IllegalArgumentException("degree must be >= 0");
        this.degree = degree;
        this.xInputs = List.copyOf(Objects.requireNonNull(xInputs, "xInputs"));
    }

    public static RunRequest of(int degree, long... xs) {
        return new RunRequest(degree, Arrays.stream(xs).boxed().toList());
    }

    // record-style accessors
    public int degree() {
        return degree;
    }

    public List<Long> xInputs() {
        return xInputs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RunRequest that)) return false;
        return degree == that.degree && xInputs.equals(that.xInputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(degree, xInputs);
    }

    @Override
    public String toString() {
        return "RunRequest[degree=%d, xInputs=%s]".formatted(degree, xInputs);
    }
}
