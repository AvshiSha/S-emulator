package com.semulator.engine.model;

public interface Label {
    String getLabel(); // label name like "L1" or "LOOP"

    int getAddress(); // instruction the label is pointing at

    default boolean isExit() {
        return false;
    }

}
