@echo off
echo Building S-Emulator Theme Test...

REM Compile the theme test
javac -cp ".;%JAVAFX_HOME%\lib\*" -d out src\ui\*.java src\ui\components\Header\*.java src\ui\components\main\*.java

if %ERRORLEVEL% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Compilation successful!
echo.
echo To run the theme test:
echo java -cp "out;%JAVAFX_HOME%\lib\*" ui.ThemeTest
echo.
echo To run the main application:
echo java -cp "out;%JAVAFX_HOME%\lib\*" ui.components.main.mainView
echo.
pause
