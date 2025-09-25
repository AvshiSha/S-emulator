package semulator.program;

import org.xml.sax.SAXException;
import semulator.instructions.SInstruction;

import javax.xml.parsers.ParserConfigurationException;
//import java.io.FileNotFoundException;
import java.io.IOException;
//import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.nio.file.Path;

public interface SProgram {

    String getName();

    void addInstruction(SInstruction instruction);

    List<SInstruction> getInstructions();

    String validate(Path xmlPath);

    int calculateMaxDegree();

    int calculateCycles();

    public ExpansionResult expandToDegree(int degree);

    Map<String, String> getFunctionUserStrings();

    Object load() throws ParserConfigurationException, IOException, SAXException;
}
