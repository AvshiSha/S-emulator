package semulator.label;

public enum FixedLabel implements Label {

    EXIT {
        @Override public String getLabel() { return "EXIT"; }
        @Override public boolean isExit() { return true; }
    },

    EMPTY {
        @Override public String getLabel() { return ""; }
    };

    @Override
    public int getAddress() {
        throw new UnsupportedOperationException(
                "FixedLabel has no concrete address; resolve at Program/Parser level.");
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
