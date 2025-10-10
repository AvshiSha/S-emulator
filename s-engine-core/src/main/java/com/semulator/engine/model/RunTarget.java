package com.semulator.engine.model;

/**
 * Represents a target for program execution (either a program or function)
 */
public class RunTarget {
    private final Type type;
    private final String name;

    public RunTarget(Type type, String name) {
        this.type = type;
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public enum Type {
        PROGRAM("Program"),
        FUNCTION("Function");

        private final String description;

        Type(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return name();
        }
    }

    @Override
    public String toString() {
        return type + ":" + name;
    }
}


