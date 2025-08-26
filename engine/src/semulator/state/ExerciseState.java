package semulator.state;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.List;

/**
 * Serializable state container for saving/loading exercise state
 */
public record ExerciseState(String xmlPath, List<Object> runHistory) implements Serializable {
    private static final long serialVersionUID = 1L;
}
