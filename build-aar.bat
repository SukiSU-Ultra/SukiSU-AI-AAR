@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "MODE=%~1"
if "%MODE%"=="" set "MODE=both"

rem The manager app picks this file up automatically. Change this path if the
rem SukiSU-Ultra checkout lives elsewhere.
set "DEPLOY_DIR=C:\Users\Shirkneko\Documents\GitHub\SukiSU-Ultra\manager\app\libs"
if defined AI_AAR_DEPLOY_DIR set "DEPLOY_DIR=%AI_AAR_DEPLOY_DIR%"

echo ============================================
echo  SukiSU AI AAR builder
echo  mode: %MODE%
echo ============================================
echo.

if /I "%MODE%"=="both" (
    call gradlew.bat :ai:assembleDebug :ai:assembleRelease
) else if /I "%MODE%"=="debug" (
    call gradlew.bat :ai:assembleDebug
) else if /I "%MODE%"=="release" (
    call gradlew.bat :ai:assembleRelease
) else (
    echo Usage: build-aar.bat [debug^|release^|both] [--no-pause]
    goto fail
)

if errorlevel 1 goto fail

if /I not "%MODE%"=="release" (
    if not exist "ai\build\outputs\aar\ai-debug.aar" (
        echo ERROR: ai-debug.aar was not produced.
        goto fail
    )
    copy /Y "ai\build\outputs\aar\ai-debug.aar" "ai.aar" >nul
    if errorlevel 1 goto fail
    echo Debug AAR  : %CD%\ai.aar
)

if /I not "%MODE%"=="debug" (
    if not exist "ai\build\outputs\aar\ai-release.aar" (
        echo ERROR: ai-release.aar was not produced.
        goto fail
    )
    copy /Y "ai\build\outputs\aar\ai-release.aar" "ai.aar" >nul
    if errorlevel 1 goto fail
    echo Release AAR: %CD%\ai.aar
)

if not exist "%DEPLOY_DIR%" (
    mkdir "%DEPLOY_DIR%"
    if errorlevel 1 (
        echo ERROR: Cannot create "%DEPLOY_DIR%"
        goto fail
    )
    echo Created deploy directory: %DEPLOY_DIR%
)

if not exist "ai.aar" (
    echo ERROR: ai.aar was not produced.
    goto fail
)

copy /Y "ai.aar" "%DEPLOY_DIR%\ai.aar" >nul
if errorlevel 1 (
    echo ERROR: Cannot copy ai.aar to "%DEPLOY_DIR%"
    goto fail
)
echo Deployed to: %DEPLOY_DIR%\ai.aar

echo.
echo Done. Output file is in the project root:
echo   %CD%\ai.aar
echo.
echo Debug source AAR: ai\build\outputs\aar\ai-debug.aar
echo Release source AAR: ai\build\outputs\aar\ai-release.aar
if /I not "%~2"=="--no-pause" pause
exit /b 0

:fail
echo.
echo BUILD FAILED
if /I not "%~2"=="--no-pause" pause
exit /b 1
