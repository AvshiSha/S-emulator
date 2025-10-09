package com.semulator.engine.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents input values for program execution
 */
public class Inputs {
    private final Map<String, Long> values;

    public Inputs() {
        this.values = new HashMap<>();
    }

    public Inputs(Map<String, Long> values) {
        this.values = new HashMap<>(values);
    }

    public void put(String key, Long value) {
        values.put(key, value);
    }

    public Long get(String key) {
        return values.get(key);
    }

    public Map<String, Long> getAll() {
        return new HashMap<>(values);
    }

    public boolean containsKey(String key) {
        return values.containsKey(key);
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }
}
