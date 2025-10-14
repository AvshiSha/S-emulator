================================================================================
S-EMULATOR SUBMISSION PACKAGE
================================================================================

This package contains all files required for Exercise submission.

CONTENTS:
---------

1. s-emulator.war
   - Complete WAR file with all dependencies included (s-engine-core, gson)
   - Ready to deploy to Tomcat's webapps directory
   - Will be accessible at: http://localhost:8080/s-emulator

2. s-client/
   - Complete client application directory
   - Contains:
     * s-client.jar - Main client application
     * lib/ - All required dependencies (18 JAR files including JavaFX, Gson, OkHttp, etc.)

3. run.bat
   - Batch file to launch the client
   - Located at the root of submission folder for easy access

================================================================================
DEPLOYMENT INSTRUCTIONS
================================================================================

SERVER DEPLOYMENT:
------------------
1. Copy s-emulator.war to Tomcat's webapps directory
   Example: C:\apache-tomcat-10.1.26\webapps\s-emulator.war

2. Start Tomcat (or it will auto-deploy if already running)
   
3. Verify deployment:
   - Check Tomcat logs for successful deployment
   - Access: http://localhost:8080/s-emulator

CLIENT DEPLOYMENT:
------------------
1. Copy the s-client directory AND run.bat to your desired location
   (Keep them in the same folder)

2. Run the client:
   - Double-click run.bat
   OR
   - From command line: run.bat

3. The client is pre-configured to connect to:
   http://localhost:8080/s-emulator

================================================================================
REQUIREMENTS SATISFIED
================================================================================

✓ Single WAR file with all dependencies included
  - Includes s-engine-core-1.0-SNAPSHOT.jar
  - Includes gson-2.10.1.jar
  - No external dependencies required

✓ Client directory with all required JARs
  - Main application: s-client/s-client.jar
  - All dependencies in s-client/lib/ directory (18 JARs)
  - Batch file provided: run.bat (at root for easy access)

✓ Automatic server recognition
  - Client hardcoded to: http://localhost:8080/s-emulator
  - No configuration needed

================================================================================
TECHNICAL DETAILS
================================================================================

Server (WAR):
- Java 17
- Jakarta Servlet API 6.0
- Includes complete engine implementation
- All dependencies packaged in WEB-INF/lib
- REST API with delta fetching support
- HTTP polling for real-time updates (no separate ports needed)

Client (s-client/):
- Java 17
- JavaFX 21 (included)
- HTTP client using OkHttp 4.12.0
- JSON processing using Gson 2.10.1
- All platform-specific native libraries included (Windows)
- Polling-based real-time updates every 2 seconds
- All communication through port 8080 (HTTP)

================================================================================
TESTING
================================================================================

1. Start Tomcat with the deployed WAR
2. Run the client using run.bat
3. Login with any username
4. All features should work including:
   - Program upload and execution
   - Debugging
   - Chat functionality
   - Real-time updates

================================================================================

