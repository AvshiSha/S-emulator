package com.semulator.engine.model;

/**
 * Represents different processor architectures
 */
public enum Architecture {
    I("Architecture I"),
    II("Architecture II"),
    III("Architecture III"),
    IV("Architecture IV");

    private final String description;

    Architecture(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public int getCost() {
        if (this == I) {
            return 5;
        } else if (this == II) {
            return 100;
        } else if (this == III) {
            return 500;
        } else if (this == IV) {
            return 1000;
        }
        return 5;
    }

    @Override
    public String toString() {
        return name();
    }
}
