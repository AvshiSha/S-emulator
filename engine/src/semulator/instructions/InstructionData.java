package semulator.instructions;

public enum InstructionData {
    INCREASE("INCREASE", 1, Type.BASIC),
    DECREASE("DECREASE", 1, Type.BASIC),
    NEUTRAL("NEUTRAL", 0, Type.BASIC),
    JUMP_NOT_ZERO("JUMP_NOT_ZERO", 2, Type.BASIC),

    ZERO_VARIABLE("ZERO", 1, Type.SYNTHETIC), // V <- 0
    GOTO_LABEL("GOTO", 1, Type.SYNTHETIC), // GOTO Lk
    ASSIGN_VARIABLE("ASSIGN", 4, Type.SYNTHETIC), // V <- V'
    ASSIGN_CONSTANT("ASSIGNC", 2, Type.SYNTHETIC), // V <- K
    JUMP_ZERO("IFZ", 2, Type.SYNTHETIC), // IF V == 0 GOTO Lk
    JUMP_EQ_CONSTANT("IFEQC", 2, Type.SYNTHETIC), // IF V == K GOTO Lk
    JUMP_EQ_VARIABLE("IFEQV", 2, Type.SYNTHETIC), // IF V == V' GOTO Lk
    QUOTE("QUOTE", 5, Type.SYNTHETIC), // V <- (Q, V1, V2, ...)
    JUMP_EQUAL_FUNCTION("JUMP_EQUAL_FUNCTION", 6, Type.SYNTHETIC); // IF V == (Q, V1, V2, ...) GOTO Lk

    public enum Type {
        BASIC, SYNTHETIC
    }

    private final String name;
    private final int cycles;
    private final Type type;

    InstructionData(String name, int cycles, Type type) {
        this.name = name;
        this.cycles = cycles;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public int getCycles() {
        return cycles;
    }

    public Type getType() {
        return type;
    }

    /** לריווח " (B|S) " בהדפסה */
    public String kindLetter() {
        return type == Type.BASIC ? "B" : "S";
    }
}