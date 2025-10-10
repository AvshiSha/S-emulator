import java.io.*;
import java.net.*;

public class TestSocket {
    public static void main(String[] args) {
        // Test if we can connect to the socket server
        try {
            System.out.println("Attempting to connect to localhost:8081...");
            Socket socket = new Socket("localhost", 8081);
            System.out.println("✓ Connected successfully!");
            
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            System.out.println("Waiting for messages...");
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Received: " + line);
            }
            
            socket.close();
        } catch (Exception e) {
            System.err.println("✗ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

