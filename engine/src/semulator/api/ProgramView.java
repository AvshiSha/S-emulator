package semulator.api;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ProgramView {
    private final String programName;
    private final List<InstructionView> instructions;
    private final Map<String, Integer> labelToIndex1Based;

    public ProgramView(String programName,
                       List<InstructionView> instructions,
                       Map<String, Integer> labelToIndex1Based) {
        this.programName = Objects.requireNonNull(programName);
        this.instructions = List.copyOf(Objects.requireNonNull(instructions));
        this.labelToIndex1Based = Map.copyOf(Objects.requireNonNull(labelToIndex1Based));
    }

    public String programName() {
        return programName;
    }

    public List<InstructionView> instructions() {
        return instructions;
    }

    public Map<String, Integer> labelToIndex1Based() {
        return labelToIndex1Based;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProgramView that)) return false;
        return programName.equals(that.programName) &&
                instructions.equals(that.instructions) &&
                labelToIndex1Based.equals(that.labelToIndex1Based);
    }

    @Override
    public int hashCode() {
        return Objects.hash(programName, instructions, labelToIndex1Based);
    }

    @Override
    public String toString() {
        return "ProgramView[name=%s, instructions=%d, labels=%d]"
                .formatted(programName, instructions.size(), labelToIndex1Based.size());
    }

    public static final class InstructionView {
        private final int index1Based;
        private final InstructionKind kind;
        private final Optional<String> label;
        private final String text;
        private final int cycles;
        private final List<String> creatorChain;

        public InstructionView(int index1Based,
                               InstructionKind kind,
                               Optional<String> label,
                               String text,
                               int cycles,
                               List<String> creatorChain) {
            this.index1Based = index1Based;
            this.kind = Objects.requireNonNull(kind);
            this.label = Objects.requireNonNull(label);
            this.text = Objects.requireNonNull(text);
            this.cycles = cycles;
            this.creatorChain = List.copyOf(Objects.requireNonNull(creatorChain));
        }

        public int index1Based() {
            return index1Based;
        }

        public InstructionKind kind() {
            return kind;
        }

        public Optional<String> label() {
            return label;
        }

        public String text() {
            return text;
        }

        public int cycles() {
            return cycles;
        }

        public List<String> creatorChain() {
            return creatorChain;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof InstructionView that)) return false;
            return index1Based == that.index1Based &&
                    cycles == that.cycles &&
                    kind == that.kind &&
                    label.equals(that.label) &&
                    text.equals(that.text) &&
                    creatorChain.equals(that.creatorChain);
        }

        @Override
        public int hashCode() {
            return Objects.hash(index1Based, kind, label, text, cycles, creatorChain);
        }

        @Override
        public String toString() {
            return "InstructionView[#%d, %s, label=%s, text=%s, cycles=%d]"
                    .formatted(index1Based, kind, label, text, cycles);
        }
    }

    public enum InstructionKind {BASIC, SYNTHETIC}
}
