@echo off
setlocal EnableExtensions
cd /d "%~dp0"

echo ==========================================
echo   Peak Cosmetics - Fabric 26.1.2 build
echo ==========================================
echo.

where java >nul 2>nul
if errorlevel 1 (
    echo [ERROR] Java/JDK not found.
    echo Install JDK 25, then reopen this BAT.
    pause
    exit /b 1
)

set "GRADLE_VERSION=9.5.1"
set "GRADLE_HOME_LOCAL=%~dp0.gradle-bin\gradle-%GRADLE_VERSION%"
set "GRADLE_ZIP=%~dp0.gradle-bin\gradle-%GRADLE_VERSION%-bin.zip"

if not exist "%GRADLE_HOME_LOCAL%\bin\gradle.bat" (
    echo [1/3] Downloading Gradle %GRADLE_VERSION%...
    if not exist "%~dp0.gradle-bin" mkdir "%~dp0.gradle-bin"
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%GRADLE_ZIP%'"
    if errorlevel 1 (
        echo [ERROR] Could not download Gradle.
        pause
        exit /b 1
    )

    echo [2/3] Extracting Gradle...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -LiteralPath '%GRADLE_ZIP%' -DestinationPath '%~dp0.gradle-bin' -Force"
    if errorlevel 1 (
        echo [ERROR] Could not extract Gradle.
        pause
        exit /b 1
    )
)

echo [3/3] Building mod...
call "%GRADLE_HOME_LOCAL%\bin\gradle.bat" clean build

if errorlevel 1 (
    echo.
    echo [ERROR] Build failed.
    pause
    exit /b 1
)

echo.
echo ==========================================
echo   BUILD COMPLETE
echo ==========================================
echo.
echo JAR files:
dir /b build\libs\*.jar
echo.
echo Put the normal JAR (not sources) into PeakMC mods folder.
echo.
pause
