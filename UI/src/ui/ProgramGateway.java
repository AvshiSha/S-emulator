package ui;

import java.nio.file.Path;
import java.util.List;

public interface ProgramGateway {
    LoadResult load(Path xmlPath);
    String show();                    // Show program בפורמט הדרוש
    String expand(int level);         // Expand בפורמט Show
    RunResult run(int level, String inputsCsv);  // מריץ ומחזיר תוצאה
    List<RunResult> history();

    record LoadResult(boolean ok, String message) {}
    record RunResult(int runNo, int level, String inputsCsv, long y, long cycles) {}
}
