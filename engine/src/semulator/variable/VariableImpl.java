package semulator.variable;

import java.util.Objects;

public class VariableImpl implements Variable {

    private final VariableType type;
    private final int number;

    public VariableImpl(VariableType type, int number) {
        if (type == null) throw new IllegalArgumentException("type is null");
        // ולידציה לפי סוג:
        if (type == VariableType.RESULT) {
            if (number != 0) {
                throw new IllegalArgumentException("RESULT variable must have number 0 (got " + number + ")");
            }
        } else {
            if (number <= 0) {
                throw new IllegalArgumentException(type + " variable index must be >= 1 (got " + number + ")");
            }
        }
        this.type = type;
        this.number = number;
    }

    @Override
    public VariableType getType() {
        return type;
    }

    @Override
    public String getRepresentation() {
        return type.getVariableRepresentation(number);
    }

    @Override
    public int getNumber() {
        return number;
    }

    /** חשוב לדיבוג/הדפסה עקבית */
    @Override
    public String toString() {
        return getRepresentation();
    }

    /** כדי לעבוד טוב עם Maps/Sets והשוואות לוגיות */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Variable)) return false;
        Variable that = (Variable) o;
        return number == that.getNumber() && type == that.getType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, number);
    }
}
