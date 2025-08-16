package semulator.api;

import java.nio.file.Path;
import java.util.List;

/**
 * Loads + validates an S-Program XML (XSD + custom validations),
 * builds an internal model, and returns a ProgramView for "Show program".
 */
public interface ProgramLoader {

    record Loaded(
            String programName,
            String loaderFingerprint,    // e.g., XSD version + instruction-table version
            Object internalModel,        // opaque to API; your AST/IR
            ProgramView programView,     // pretty view for "Show program"
            List<String> warnings
    ) {
    }

    Loaded load(Path xmlPath) throws LoadException;

    class LoadException extends Exception {
        private final List<String> errors;
        private final List<String> warnings;

        public LoadException(List<String> errors, List<String> warnings, Throwable cause) {
            super(String.join("\n", errors), cause);
            this.errors = List.copyOf(errors);
            this.warnings = List.copyOf(warnings);
        }

        public List<String> errors() {
            return errors;
        }

        public List<String> warnings() {
            return warnings;
        }
    }
}
