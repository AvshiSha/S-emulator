package semulator.api;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class LoadResult {

    private final boolean success;
    private final Optional<ProgramHandle> program;
    private final List<String> warnings;
    private final List<String> errors;

    private LoadResult(boolean success, Optional<ProgramHandle> program,
                       List<String> warnings, List<String> errors) {
        this.success = success;
        this.program = Objects.requireNonNull(program);
        this.warnings = List.copyOf(Objects.requireNonNull(warnings));
        this.errors = List.copyOf(Objects.requireNonNull(errors));
    }

    public static LoadResult ok(ProgramHandle handle, List<String> warnings) {
        return new LoadResult(true, Optional.of(handle), List.copyOf(warnings), List.of());
    }

    public static LoadResult fail(List<String> errors, List<String> warnings) {
        return new LoadResult(false, Optional.empty(), List.copyOf(warnings), List.copyOf(errors));
    }

    // Nested ProgramHandle (also as a class)
    public static final class ProgramHandle {
        private final String programName;
        private final Path sourcePath;
        private final Instant loadedAt;
        private final String loaderFingerprint;

        public ProgramHandle(String programName, Path sourcePath, Instant loadedAt, String loaderFingerprint) {
            this.programName = Objects.requireNonNull(programName);
            this.sourcePath = Objects.requireNonNull(sourcePath);
            this.loadedAt = Objects.requireNonNull(loadedAt);
            this.loaderFingerprint = Objects.requireNonNull(loaderFingerprint);
        }
    }
}

