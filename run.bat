@echo off
echo Starting S-Emulator...

REM Check if JavaFX is available in the system
java --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Java is not installed or not in PATH
    pause
    exit /b 1
)

REM Try to run with JavaFX modules (Java 11+)
java --module-path "C:\Program Files\Java\javafx-17.0.2\lib" --add-modules javafx.controls,javafx.fxml -jar ui.jar
if %errorlevel% equ 0 goto :success

REM If the above fails, try with local JavaFX
java --module-path "lib\javafx" --add-modules javafx.controls,javafx.fxml -jar ui.jar
if %errorlevel% equ 0 goto :success

REM If still fails, try without module path (might work if JavaFX is in classpath)
java -jar ui.jar
if %errorlevel% equ 0 goto :success

echo Failed to start application. JavaFX runtime not found.
echo Please install JavaFX or use the run-with-javafx.bat script.
pause
exit /b 1

:success
echo Application started successfully!
pause



