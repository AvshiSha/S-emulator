package com.semulator.engine.model;

import java.io.Serializable;
import java.util.List;

/**
 * Serializable state container for saving/loading exercise state
 */
public record ExerciseState(String xmlPath, List<Object> runHistory) implements Serializable {
    private static final long serialVersionUID = 1L;
}

