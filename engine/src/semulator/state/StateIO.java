// Put in semulator.state
package semulator.state;

import java.io.*;
import java.nio.file.*;

public final class StateIO {
    private StateIO() {
    }

    private static Path withExt(Path base) {
        return base.resolveSibling(base.getFileName() + ".sex");
    }

    public static void save(Path basePathNoExt, ExerciseState state) throws IOException {
        try (var out = new ObjectOutputStream(Files.newOutputStream(withExt(basePathNoExt)))) {
            out.writeObject(state);
        }
    }

    public static ExerciseState load(Path basePathNoExt) throws IOException, ClassNotFoundException {
        try (var in = new ObjectInputStream(Files.newInputStream(withExt(basePathNoExt)))) {
            return (ExerciseState) in.readObject();
        }
    }
}
