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
    
    @Override
    public String toString() {
        return name();
    }
}
