package ui;

import org.xml.sax.SAXException;
import semulator.program.SProgramImpl;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            // Start the console UI
            new ConsoleUI(new SProgramImpl("S")).start();
        } catch (IOException e) {
            System.err.println("I/O error occurred: " + e.getMessage());
            System.exit(1);
        } catch (ParserConfigurationException e) {
            System.err.println("XML parser configuration error: " + e.getMessage());
            System.exit(1);
        } catch (SAXException e) {
            System.err.println("XML parsing error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

}