package semulator.program;

import org.xml.sax.SAXException;
import semulator.instructions.SInstruction;

import javax.xml.parsers.ParserConfigurationException;
//import java.io.FileNotFoundException;
import java.io.IOException;
//import java.nio.file.Path;
import java.util.List;

public interface SProgram {

    String getName();

    void addInstruction(SInstruction instruction);

    List<SInstruction> getInstructions();

    boolean validate();

    int calculateMaxDegree();

    int calculateCycles();

    Object load() throws ParserConfigurationException, IOException, SAXException;
}
