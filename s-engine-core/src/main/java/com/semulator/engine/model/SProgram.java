package com.semulator.engine.model;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface SProgram {

    String getName();

    void addInstruction(SInstruction instruction);

    List<SInstruction> getInstructions();

    String validate(Path xmlPath);

    int calculateMaxDegree();

    /**
     * Calculate the template degree of a function (no +1 for call-site expansion).
     * This is used when we want to know the internal complexity of a function
     * without the call-site expansion layer.
     */
    int calculateFunctionTemplateDegree(String functionName);

    int calculateCycles();

    public ExpansionResult expandToDegree(int degree);

    /**
     * Expand a specific function to a given degree.
     * This allows expanding individual functions instead of the entire main
     * program.
     */
    public ExpansionResult expandFunctionToDegree(String functionName, int degree);

    Map<String, String> getFunctionUserStrings();

    Object load() throws ParserConfigurationException, IOException, SAXException;
}
