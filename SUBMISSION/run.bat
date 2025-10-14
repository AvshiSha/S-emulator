@echo off
setlocal enabledelayedexpansion

REM ====================================
REM S-Emulator Client Launcher
REM ====================================

echo Starting S-Emulator Client...
echo.

REM Check if Java is installed
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 17 or higher
    pause
    exit /b 1
)

REM Get the directory where this batch file is located
set "APP_DIR=%~dp0"

REM Set JavaFX module path
set "JAVAFX_PATH=%APP_DIR%s-client\lib\javafx-sdk-22.0.2\lib"

REM Build the classpath (excluding JavaFX JARs from classpath since they'll be in module path)
set "CLASSPATH=%APP_DIR%s-client\s-client.jar"
for %%i in ("%APP_DIR%s-client\lib\*.jar") do (
    set "CLASSPATH=!CLASSPATH!;%%i"
)

REM Run the application with JavaFX module path
REM The server is automatically configured to connect to http://localhost:8080/s-emulator
java --module-path "!JAVAFX_PATH!" --add-modules javafx.controls,javafx.fxml -cp "!CLASSPATH!" com.semulator.client.Main

REM Check if the application exited with an error
if errorlevel 1 (
    echo.
    echo ERROR: Application failed to start
    pause
    exit /b 1
)

endlocal

