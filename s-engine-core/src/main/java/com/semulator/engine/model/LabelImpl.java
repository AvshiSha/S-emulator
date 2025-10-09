package com.semulator.engine.model;

public class LabelImpl implements Label {

    private final String label; // לדוגמה "L3"
    private final Integer address; // null => לא פתור עדיין

    public LabelImpl(int number, int address) {
        if (number < 0)
            throw new IllegalArgumentException("label number must be non-negative");
        if (address < 0)
            throw new IllegalArgumentException("address must be non-negative");
        this.label = "L" + number;
        this.address = address;
    }

    public LabelImpl(int number) {
        if (number < 0)
            throw new IllegalArgumentException("label number must be non-negative");
        this.label = "L" + number;
        this.address = null; // עדיין לא נקבע
    }

    public LabelImpl(String label) {
        if (label == null)
            throw new IllegalArgumentException("label is null");
        this.label = label;
        this.address = null;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public int getAddress() {
        if (address == null) {
            throw new IllegalStateException(
                    "Label '" + label + "' address is not resolved yet. " +
                            "Use the ctor with address or resolve in Program/Parser.");
        }
        return address;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
