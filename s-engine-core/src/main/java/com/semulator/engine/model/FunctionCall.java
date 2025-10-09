package com.semulator.engine.model;

import java.util.List;

/**
 * Represents a function call that can be used as an argument in composition.
 * This enables nested function calls like (AND,(NOT,(EQUAL,x1,x2)),(CONST0))
 */
public class FunctionCall {
    private final String functionName;
    private final List<FunctionArgument> arguments;

    public FunctionCall(String functionName, List<FunctionArgument> arguments) {
        if (functionName == null || functionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Function name cannot be null or empty");
        }
        if (arguments == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        this.functionName = functionName.trim();
        this.arguments = arguments;
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<FunctionArgument> getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(functionName);
        for (FunctionArgument arg : arguments) {
            sb.append(",").append(arg.toString());
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        FunctionCall that = (FunctionCall) obj;
        return functionName.equals(that.functionName) && arguments.equals(that.arguments);
    }

    @Override
    public int hashCode() {
        return functionName.hashCode() * 31 + arguments.hashCode();
    }
}
