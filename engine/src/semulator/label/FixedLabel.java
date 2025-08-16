package semulator.label;

public enum FixedLabel implements Label {

    EXIT {
        @Override
        public String getLabelRepresentation() {
            return "EXIT";
        }
    },
    EMPTY {
        @Override
        public String getLabelRepresentation() {
            return "";
        }
    };

    @Override
    public abstract String getLabelRepresentation();
}