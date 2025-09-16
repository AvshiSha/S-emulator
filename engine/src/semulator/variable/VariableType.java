package semulator.variable;

public enum VariableType {
    RESULT {
        @Override
        public String getVariableRepresentation(int number) {
            return "y";
        }
    },
    INPUT {
        @Override
        public String getVariableRepresentation(int number) {
            return "x" + number;
        }
    },
    WORK {
        @Override
        public String getVariableRepresentation(int number) {
            return "z" + number;
        }
    },
    Constant {
        @Override
        public String getVariableRepresentation(int number) {
            return number + "";
        }
    };

    public abstract String getVariableRepresentation(int number);
}