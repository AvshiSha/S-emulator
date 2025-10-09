package com.semulator.engine.model;

/**
 * Represents a function call argument (composition case).
 * This enables nested function calls as arguments.
 */
public class FunctionCallArgument extends FunctionArgument {
    private final FunctionCall functionCall;

    public FunctionCallArgument(FunctionCall functionCall) {
        if (functionCall == null) {
            throw new IllegalArgumentException("Function call cannot be null");
        }
        this.functionCall = functionCall;
    }

    public FunctionCallArgument(String functionName, java.util.List<FunctionArgument> arguments) {
        this(new FunctionCall(functionName, arguments));
    }

    @Override
    public boolean isFunctionCall() {
        return true;
    }

    @Override
    public Variable asVariable() {
        throw new UnsupportedOperationException("This is a function call argument, not a variable");
    }

    @Override
    public FunctionCall asFunctionCall() {
        return functionCall;
    }

    public FunctionCall getFunctionCall() {
        return functionCall;
    }

    @Override
    public String toString() {
        return functionCall.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        FunctionCallArgument that = (FunctionCallArgument) obj;
        return functionCall.equals(that.functionCall);
    }

    @Override
    public int hashCode() {
        return functionCall.hashCode();
    }
}
