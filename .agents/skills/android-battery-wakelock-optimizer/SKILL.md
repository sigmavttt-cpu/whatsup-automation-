---
name: android-battery-wakelock-optimizer
description: Guides 24/7 Android foreground service survival, WakeLock lifecycle, Doze Mode bypass, OEM battery killer mitigation (Samsung OneUI, Xiaomi MIUI), and preventing 100% CPU drain. Trigger on "battery", "wakelock", "doze", "بطارية", "استهلاك المعالج", "قتل الخدمة في الخلفية", "ForegroundServiceType", "WakeLock", "PowerManager", "Samsung battery", "Xiaomi battery", "Doze mode".
---

# Android Battery & WakeLock Optimization Protocol

## 1. Core Mission
Keeps WhatsUp Automation's background engine (`WhatsAppForegroundService`) alive 24/7 across modern Android versions (Android 10 through 15) while strictly avoiding thermal throttling, high battery drain, and OEM background app killers (e.g. Samsung Sleeping Apps, Xiaomi MIUI autostart restrictions).

---

## 2. The 4 Golden Rules of WakeLock Management

### I. Never Hold an Unbounded WakeLock
- **The Bug That Burned 102% CPU:** Holding a permanent `wakeLock.acquire()` without timeout or releasing during idle loops causes the CPU to stay at maximum clock speed, burning battery and triggering Android OS battery warnings.
- **The Correct Pattern:** Use timed acquires or scoped acquires:
  ```kotlin
  // Only acquire during active network payload exchange or pairing
  wakeLock.acquire(30_000L) // Safe auto-release after 30 seconds
  ```

### II. Decouple Render-Phase from Polling
- Never run UI polling loops faster than 15 seconds when the app is in the background or screen is off.
- Listen for screen state broadcasts (`ACTION_SCREEN_OFF` / `ACTION_SCREEN_ON`) to throttle health-check intervals:
  - **Screen ON:** Telemetry update every 4-5 seconds.
  - **Screen OFF:** Deep sleep throttle every 30-60 seconds.

### III. Request Battery Optimization Exemption Cleanly
```kotlin
fun requestIgnoreBatteryOptimizations(context: Context) {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val packageName = context.packageName
    
    if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
```

### IV. Android 14+ Foreground Service Types
In Android 14 (API 34) and Android 15 (API 35), every Foreground Service must declare explicit types in `AndroidManifest.xml`:
```xml
<service
    android:name=".service.WhatsAppForegroundService"
    android:foregroundServiceType="dataSync|connectedDevice"
    android:exported="false" />
```
And pass the type when calling `startForeground`:
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    ServiceCompat.startForeground(
        this,
        NOTIFICATION_ID,
        notification,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        } else {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        }
    )
}
```

---

## 3. OEM Aggressive Killer Mitigation Checklist

| Manufacturer | Known Killer Mechanism | User Guidance Strategy |
| :--- | :--- | :--- |
| **Samsung (OneUI)** | Puts apps into "Deep Sleeping Apps" after 3 days of no direct UI opens. | Guide user to: *Settings -> Apps -> WhatsUp -> Battery -> Unrestricted*. |
| **Xiaomi (MIUI/HyperOS)** | Kills background sockets within 5 minutes unless Autostart is allowed. | Guide user to: *Security App -> Manage Apps -> WhatsUp -> Autostart: ON* + *Battery Saver: No Restrictions*. |
| **Huawei / Honor** | PowerGenie kills foreground services without ongoing notification updates. | Keep notification progress bar or status text updating periodically (e.g. heartbeat timestamp). |

---

## 4. Diagnostics & CPU Burn Verification
To check if the service is burning CPU or leaking WakeLocks via ADB:
```bash
# Check CPU consumption of WhatsUp process
adb shell top -b -n 1 | grep com.whatsup.automation

# Check active WakeLocks held by the system
adb shell dumpsys power | grep -A 10 "Wake Locks"

# Check battery stats and alarms
adb shell dumpsys batterystats --charged com.whatsup.automation
```
