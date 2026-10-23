param(
    [switch]$Launch = $true
)

$adbPath = "C:\Users\sO377\AppData\Local\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adbPath)) {
    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) { $adbPath = $cmd.Source }
}

if (-not (Test-Path $adbPath)) {
    Write-Host "[ERROR] adb.exe not found" -ForegroundColor Red
    exit 1
}

$apkPath = Join-Path $PSScriptRoot "..\app\build\outputs\apk\debug\app-debug.apk"
$apkPath = [System.IO.Path]::GetFullPath($apkPath)

if (-not (Test-Path $apkPath)) {
    Write-Host "[ERROR] APK not found: $apkPath" -ForegroundColor Red
    exit 1
}

Write-Host "[INFO] Ensuring ADB server & device ready..." -ForegroundColor Cyan
& $adbPath start-server | Out-Null
Start-Sleep -Seconds 2
& $adbPath wait-for-device

Write-Host "[INFO] Installing APK on device..." -ForegroundColor Cyan
& $adbPath install -r -d $apkPath

if ($LASTEXITCODE -eq 0 -and $Launch) {
    Write-Host "[INFO] Starting application..." -ForegroundColor Green
    & $adbPath shell am start -n "com.sigmacodex.whatsup/com.whatsup.automation.presentation.MainActivity"
}
