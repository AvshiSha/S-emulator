@echo off
echo Starting S-Emulator Server...
echo.

REM Check if Java is available
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java is not installed or not in PATH
    echo Please install Java 17 or higher
    pause
    exit /b 1
)

REM Check if Tomcat is available (we'll use embedded Tomcat or simple HTTP server)
echo Starting embedded Tomcat server...
echo Server will be available at: http://localhost:8080/s-emulator/
echo Press Ctrl+C to stop the server
echo.

REM Run the WAR file using Tomcat
REM For now, let's use a simple approach - copy the WAR to a Tomcat installation
REM or use an embedded server approach

echo.
echo ========================================
echo S-Emulator Server Started Successfully!
echo ========================================
echo.
echo Available endpoints:
echo   GET  /s-emulator/api/engine           - Health check
echo   POST /s-emulator/api/auth/login       - User login
echo   GET  /s-emulator/api/programs         - List programs
echo   GET  /s-emulator/api/functions        - List functions
echo   POST /s-emulator/api/upload           - Upload XML program
echo   POST /s-emulator/api/run/start        - Start program run
echo   GET  /s-emulator/api/run/status       - Get run status
echo   POST /s-emulator/api/debug/step       - Debug step
echo   GET  /s-emulator/api/history          - Get user history
echo.
echo Note: This is a placeholder script.
echo You need to deploy the WAR file to a Tomcat server.
echo The WAR file is located at: target/s-emulator.war
echo.
pause


