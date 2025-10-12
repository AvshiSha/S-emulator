package com.semulator.client.ui.components;

import com.semulator.engine.model.*;

/**
 * Enum representing processor architectures with different instruction support
 * levels
 */
public enum Architecture {
    I("I"),
    II("II"),
    III("III"),
    IV("IV");

    private final String code;

    Architecture(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * Determines the minimum architecture required for a given instruction
     */
    public static Architecture fromInstruction(SInstruction instruction) {
        if (instruction == null) {
            return I;
        }

        // Architecture I: Basic instructions
        if (instruction instanceof IncreaseInstruction ||
                instruction instanceof DecreaseInstruction ||
                instruction instanceof NoOpInstruction ||
                instruction instanceof JumpNotZeroInstruction) {
            return I;
        }

        // Architecture II: Architecture I + Zero, Constant Assignment, Goto
        if (instruction instanceof ZeroVariableInstruction ||
                instruction instanceof AssignConstantInstruction ||
                instruction instanceof GotoLabelInstruction) {
            return II;
        }

        // Architecture III: Architecture II + Assignment, Jump Zero, Jump Equal
        if (instruction instanceof AssignVariableInstruction ||
                instruction instanceof JumpZeroInstruction ||
                instruction instanceof JumpEqualConstantInstruction ||
                instruction instanceof JumpEqualVariableInstruction) {
            return III;
        }

        // Architecture IV: Architecture III + Quote, Jump Equal Function
        if (instruction instanceof QuoteInstruction ||
                instruction instanceof JumpEqualFunctionInstruction) {
            return IV;
        }

        // Default to Architecture I for unknown instructions
        return I;
    }

    @Override
    public String toString() {
        return code;
    }
}
