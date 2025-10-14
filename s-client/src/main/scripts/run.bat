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

REM Build the classpath
set "CLASSPATH=%APP_DIR%s-client.jar"
for %%i in ("%APP_DIR%lib\*.jar") do (
    set "CLASSPATH=!CLASSPATH!;%%i"
)

REM Run the application
REM The server is automatically configured to connect to http://localhost:8080/s-emulator
java -cp "!CLASSPATH!" com.semulator.client.Main

REM Check if the application exited with an error
if errorlevel 1 (
    echo.
    echo ERROR: Application failed to start
    pause
    exit /b 1
)

endlocal

