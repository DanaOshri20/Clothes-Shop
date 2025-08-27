@echo off
chcp 65001 >nul
echo === Compiling project ===

REM Clean and recreate output directory
if exist out rmdir /s /q out
mkdir out

javac -version

REM Compile all Java files with proper package structure
javac -d out -cp src ^
  src\client\app\*.java ^
  src\server\app\*.java ^
  src\server\domain\customers\*.java ^
  src\server\domain\employees\*.java ^
  src\server\domain\invantory\*.java ^
  src\server\domain\sales\*.java ^
  src\server\net\*.java ^
  src\server\shared\*.java ^
  src\server\util\*.java

if %errorlevel% neq 0 (
    echo.
    echo *** Compilation failed ***
    pause
    exit /b %errorlevel%
)

echo.
echo === Compilation finished successfully ===
echo Output directory structure:
dir /s /b out\*.class | findstr /n . | findstr /b "^[1-9][0-9]*:" | findstr /b "^[1-9]:"
echo.
pause
