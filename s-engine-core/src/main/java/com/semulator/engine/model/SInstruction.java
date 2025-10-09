package com.semulator.engine.model;

import com.semulator.engine.exec.ExecutionContext;

public interface SInstruction {

    String getName();

    Label execute(ExecutionContext context);

    int cycles();

    Label getLabel();

    Variable getVariable();
}
