package semulator.program;

import semulator.instructions.SInstruction;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

public class SProgramImpl implements SProgram {

    private final String name;
    private final List<SInstruction> instructions;

    // Allowed instruction names (case-sensitive)
    private static final Set<String> ALLOWED_NAMES = Set.of(
            "NEUTRAL", "INCREASE", "DECREASE", "JUMP_NOT_ZERO",
            "ZERO_VARIABLE", "ASSIGNMENT", "GOTO_LABEL", "CONSTANT_ASSIGNMENT",
            "JUMP_ZERO", "JUMP_EQUAL_CONSTANT", "JUMP_EQUAL_VARIABLE"
    );

    public SProgramImpl(String name) {
        this.name = name;
        instructions = new ArrayList<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addInstruction(SInstruction instruction) {
        instructions.add(instruction);
    }

    @Override
    public List<SInstruction> getInstructions() {
        return instructions;
    }

    @Override
    public boolean validate() {
        return false;
    }

    @Override
    public int calculateMaxDegree() {
        // traverse all commands and find the maximum degree
        return 0;
    }

    @Override
    public int calculateCycles() {
        // traverse all commands and calculate cycles
        return 0;
    }
}

