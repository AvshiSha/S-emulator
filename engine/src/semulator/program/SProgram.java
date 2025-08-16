package semulator.program;

import semulator.instructions.SInstruction;

import java.util.List;

public interface SProgram {

    String getName();

    void addInstruction(SInstruction instruction);

    List<SInstruction> getInstructions();

    boolean validate();

    int calculateMaxDegree();

    int calculateCycles();

}
