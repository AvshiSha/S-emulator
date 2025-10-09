package com.semulator.engine.model;

/**
 * Represents a simple variable argument (non-composition case).
 * This is the current behavior where arguments are just variables.
 */
public class VariableArgument extends FunctionArgument {
    private final Variable variable;

    public VariableArgument(Variable variable) {
        if (variable == null) {
            throw new IllegalArgumentException("Variable cannot be null");
        }
        this.variable = variable;
    }

    @Override
    public boolean isFunctionCall() {
        return false;
    }

    @Override
    public Variable asVariable() {
        return variable;
    }

    @Override
    public FunctionCall asFunctionCall() {
        throw new UnsupportedOperationException("This is a variable argument, not a function call");
    }

    public Variable getVariable() {
        return variable;
    }

    @Override
    public String toString() {
        return variable.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        VariableArgument that = (VariableArgument) obj;
        return variable.equals(that.variable);
    }

    @Override
    public int hashCode() {
        return variable.hashCode();
    }
}
