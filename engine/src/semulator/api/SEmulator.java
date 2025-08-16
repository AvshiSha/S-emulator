package semulator.api;

import java.nio.file.Path;
import java.util.Optional;

/**
 * The public façade for the S-emulator engine.
 * - External/UI-facing indices are 1-based.
 * - Engine is passive: returns data/strings, never prints.
 */
public interface SEmulator {

    /**
     * Load and validate an S-program XML. On success, this becomes the "current" program.
     * If loading fails, the previously loaded valid program (if any) remains current.
     */
    LoadResult loadProgram(Path xmlPath);

    /**
     * @return a view of the currently loaded program (unexpanded), if any.
     */
    Optional<ProgramView> currentProgram();

    /**
     * Run the CURRENT program with the given request (degree, x-inputs).
     * Returns a detailed result including y, used variables (ordered), per-instruction cycles,
     * total cycles, and (optionally) an expanded program view if the runner provides one.
     */
    RunResult run(RunRequest request);

    /**
     * @return accumulated run history since façade construction or last clear.
     */
    History history();

    /**
     * Clears the in-memory history (does NOT affect the current program).
     */
    void clearHistory();
}
