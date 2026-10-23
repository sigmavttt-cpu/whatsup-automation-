@echo off
chcp 65001 >nul
set "ADB=C:\Users\sO377\AppData\Local\Android\Sdk\platform-tools\adb.exe"
set "APK=%~dp0..\app\build\outputs\apk\debug\app-debug.apk"

if not exist "%ADB%" (
    echo [ERROR] adb.exe not found at %ADB%
    pause
    exit /b 1
)

if not exist "%APK%" (
    echo [ERROR] app-debug.apk not found at %APK%
    pause
    exit /b 1
)

echo ========================================================
echo        WhatsUp Automation - Fast USB Installer
echo ========================================================
echo [1/3] Checking connected devices...
"%ADB%" devices
echo.

echo [2/3] Installing APK (25.61 MB) to phone...
"%ADB%" install -r -d "%APK%"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [FAILED] Please check:
    echo 1. USB Debugging is ON in Developer Options.
    echo 2. Accept the 'Allow USB Debugging' popup on your phone screen.
    echo.
    exit /b 1
)

echo.
echo [3/3] Launching app on phone...
"%ADB%" shell am start -n com.sigmacodex.whatsup/com.whatsup.automation.presentation.MainActivity
echo.
echo [SUCCESS] WhatsUp Automation is now active on your phone!
