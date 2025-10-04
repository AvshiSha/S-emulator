@echo off
setlocal

REM --- Resolve folder of this script ---
set "SCRIPT_DIR=%~dp0"

REM --- JavaFX jars (relative path) ---
set "FX_DIR=%SCRIPT_DIR%lib\javafx-sdk-22.0.2\lib"

REM --- All your jars (UI + engine + deps) ---
set "CP=%SCRIPT_DIR%UI.jar;%SCRIPT_DIR%engine.jar;%SCRIPT_DIR%lib\deps\*"

REM --- Main class (since you don’t have one fat jar with a MANIFEST) ---
set "MAIN_CLASS=ui.components.main.mainView"

REM --- Prefer JAVA_HOME if available ---
set "JAVA_EXE=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"

echo Launching S-Emulator...
"%JAVA_EXE%" --module-path "%FX_DIR%" --add-modules javafx.controls,javafx.fxml,javafx.graphics ^
  -cp "%CP%" %MAIN_CLASS%

pause
endlocal
