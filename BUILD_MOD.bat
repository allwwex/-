@echo off
setlocal
cd /d "%~dp0"

echo ==========================================
echo   Peak Cosmetics - Fabric 26.1.2 build
echo ==========================================
echo.

if exist "gradlew.bat" (
    call gradlew.bat clean build
) else (
    where gradle >nul 2>nul
    if errorlevel 1 (
        echo [ERROR] Gradle wrapper is missing and Gradle is not installed.
        echo Use the prepared ZIP that contains gradle\wrapper\gradle-wrapper.jar.
        pause
        exit /b 1
    )
    call gradle clean build
)

if errorlevel 1 (
    echo.
    echo [ERROR] Build failed.
    pause
    exit /b 1
)

echo.
echo [OK] JAR created in build\libs\
dir /b build\libs\*.jar
echo.
pause
