package semulator.program;

import semulator.instructions.SInstruction;

import java.util.List;
import java.util.Map;

public record ExpansionResult(
        List<SInstruction> instructions,
        Map<SInstruction, SInstruction> parent,   // immediate creator
        Map<SInstruction, Integer> lineNo,
        Map<SInstruction, Integer> rowOf// 1-based numbering in this snapshot
) {
}