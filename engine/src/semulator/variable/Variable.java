package semulator.variable;

public interface Variable {
    VariableType getType();

    /** x1 / z2 / y */
    String getRepresentation();

    /** המונה של המשתנה: RESULT -> 0, INPUT/WORK -> >=1 */
    int getNumber();

    /** עזרי זיהוי (לא חובה, אבל נוח): */
    default boolean isResult() {
        return getType() == VariableType.RESULT;
    }

    default boolean isInput() {
        return getType() == VariableType.INPUT;
    }

    default boolean isWork() {
        return getType() == VariableType.WORK;
    }

    default boolean isConstant() {
        return getType() == VariableType.Constant;
    }

    /** קבוע נוח ל-y */
    Variable RESULT = new VariableImpl(VariableType.RESULT, 0);

    /**
     * instead of:
     * Variable v = new VariableImpl(VariableType.INPUT, 3);
     * we can directly read:
     * Variable v = Variable.of("x3");
     */
    static Variable of(String token) {
        if (token == null)
            throw new IllegalArgumentException("null variable token");
        String t = token.trim();
        if (t.equals("y"))
            return RESULT;
        if (t.startsWith("x")) {
            int n = Integer.parseInt(t.substring(1));
            return new VariableImpl(VariableType.INPUT, n);
        }
        if (t.startsWith("z")) {
            int n = Integer.parseInt(t.substring(1));
            return new VariableImpl(VariableType.WORK, n);
        }
        throw new IllegalArgumentException("Invalid variable token: " + token);
    }
}
