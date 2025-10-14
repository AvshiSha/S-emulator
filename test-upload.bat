@echo off
echo Testing Server Upload with Reminder.xml
echo ========================================
echo.
echo Make sure the server is running first!
echo Server should be running on: http://localhost:8080/s-emulator
echo.
pause
echo.
echo Uploading test_reminder.xml to server...
echo.

curl -X POST http://localhost:8080/s-emulator/api/upload ^
  -H "Content-Type: application/json" ^
  -d "{\"filename\":\"test_reminder.xml\",\"content\":\"<?xml version='1.0' encoding='UTF-8'?><S-Program xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' name='Reminder' xsi:noNamespaceSchemaLocation='S-Emulator-v2.xsd'><S-Instructions><S-Instruction type='synthetic' name='JUMP_ZERO'><S-Variable>x2</S-Variable><S-Instruction-Arguments><S-Instruction-Argument name='JZLabel' value='EXIT'/></S-Instruction-Arguments></S-Instruction><S-Instruction type='synthetic' name='ASSIGNMENT'><S-Variable>z3</S-Variable><S-Instruction-Arguments><S-Instruction-Argument name='assignedVariable' value='x1'/></S-Instruction-Arguments></S-Instruction><S-Instruction type='synthetic' name='QUOTE'><S-Variable>z2</S-Variable><S-Instruction-Arguments><S-Instruction-Argument name='functionName' value='Smaller_Than'/><S-Instruction-Argument name='functionArguments' value='x1,x2'/></S-Instruction-Arguments></S-Instruction><S-Instruction type='basic' name='JUMP_NOT_ZERO'><S-Variable>z2</S-Variable><S-Instruction-Arguments><S-Instruction-Argument name='JNZLabel' value='L2'/></S-Instruction-Arguments></S-Instruction><S-Instruction type='synthetic' name='QUOTE'><S-Variable>z3</S-Variable><S-Label>L1</S-Label><S-Instruction-Arguments><S-Instruction-Argument name='functionName' value='Minus'/><S-Instruction-Argument name='functionArguments' value='z3,x2'/></S-Instruction-Arguments></S-Instruction><S-Instruction type='basic' name='INCREASE'><S-Variable>z4</S-Variable></S-Instruction><S-Instruction type='synthetic' name='QUOTE'><S-Variable>z1</S-Variable><S-Instruction-Arguments><S-Instruction-Argument name='functionName' value='AND'/><S-Instruction-Argument name='functionArguments' value='(Bigger_Equal_Than,z3,x2),(NOT,(EQUAL,z3,(CONST0)))'/></S-Instruction-Arguments></S-Instruction><S-Instruction type='basic' name='JUMP_NOT_ZERO'><S-Variable>z1</S-Variable><S-Instruction-Arguments><S-Instruction-Argument name='JNZLabel' value='L1'/></S-Instruction-Arguments></S-Instruction><S-Instruction type='synthetic' name='ASSIGNMENT'><S-Variable>y</S-Variable><S-Label>L2</S-Label><S-Instruction-Arguments><S-Instruction-Argument name='assignedVariable' value='z3'/></S-Instruction-Arguments></S-Instruction></S-Instructions><S-Functions><S-Function name='CONST0' user-string='C0'><S-Instructions><S-Instruction type='synthetic' name='CONSTANT_ASSIGNMENT'><S-Variable>y</S-Variable><S-Instruction-Arguments><S-Instruction-Argument name='constantValue' value='0'/></S-Instruction-Arguments></S-Instruction></S-Instructions></S-Function></S-Functions></S-Program>\"}"

echo.
echo.
echo Check the SERVER CONSOLE for validation logging!
echo.
pause

