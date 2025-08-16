package semulator.variable;

public interface Variable {
    VariableType getType();

    String getRepresentation();

    Variable RESULT = new VariableImpl(VariableType.RESULT, 0);
}
