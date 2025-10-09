package com.semulator.engine.model;

/**
 * Abstract base class for function arguments that can be either simple
 * variables
 * or nested function calls (composition mechanism).
 */
public abstract class FunctionArgument {

    /**
     * Check if this argument is a function call (composition)
     */
    public abstract boolean isFunctionCall();

    /**
     * Get this argument as a simple variable (throws if not a variable)
     */
    public abstract Variable asVariable();

    /**
     * Get this argument as a function call (throws if not a function call)
     */
    public abstract FunctionCall asFunctionCall();

    /**
     * Get a string representation for debugging
     */
    public abstract String toString();
}
