package semulator.api;

import semulator.api.LoadResult.ProgramHandle;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

public class SEmulatorDefault implements SEmulator {
    private final ProgramLoader loader;
    private final ProgramRunner runner;

    private LoadResult.ProgramHandle currentHandle;       // last successfully loaded
    private Object internalProgramModel;       // opaque; provided by loader
    private ProgramView currentProgramView;    // unexpanded view for "Show program"

    private final List<History.Entry> history = new ArrayList<>();

    public SEmulatorDefault(ProgramLoader loader, ProgramRunner runner) {
        this.loader = Objects.requireNonNull(loader);
        this.runner = Objects.requireNonNull(runner);
    }


    @Override
    public LoadResult loadProgram(Path xmlPath) {
        try {
            ProgramLoader.Loaded loaded = loader.load(xmlPath);
            // success -> swap current
            this.currentHandle = new ProgramHandle(
                    loaded.programName(),
                    xmlPath,
                    Instant.now(),
                    loaded.loaderFingerprint()
            );
            this.internalProgramModel = loaded.internalModel(); // opaque to API
            this.currentProgramView = loaded.programView();     // unexpanded baseline
            return LoadResult.ok(currentHandle, loaded.warnings());
        } catch (ProgramLoader.LoadException e) {
            // failure -> keep previous program, report errors/warnings
            return LoadResult.fail(e.errors(), e.warnings());
        }
    }


    @Override
    public Optional<ProgramView> currentProgram() {
        return Optional.ofNullable(currentProgramView);
    }

    @Override
    public RunResult run(RunRequest request) {
        if (internalProgramModel == null) {
            throw new SEmulatorException("No program loaded. Load a valid S-Program XML first.");
        }
        ProgramRunner.RunOutcome out = runner.run(internalProgramModel, request);

        // Record history (1-based indexing externally)
        History.Entry entry = new History.Entry(
                history.size() + 1,
                Instant.now(),
                currentHandle.programName(),
                request.degree(),
                List.copyOf(request.xInputs()),
                out.y(),
                out.totalCycles(),
                out.stepsExecuted()
        );
        history.add(entry);

        return new RunResult(
                out.y(),
                RunResult.orderedVars(out.usedVariablesOrdered()), // enforce order y, x_i, z_i
                out.totalCycles(),
                out.stepsExecuted(),
                out.programViewShown(),
                List.copyOf(out.notes())
        );
    }

    @Override
    public History history() {
        return () -> List.copyOf(history);
    }

    @Override
    public void clearHistory() {
        history.clear();
    }
}