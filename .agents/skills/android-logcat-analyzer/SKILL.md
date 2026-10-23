---
name: android-logcat-analyzer
description: >-
  Use this skill to capture, filter, diagnose, and resolve Android runtime errors,
  crashes, ANRs, memory kills (LMK), and service lifecycles for WhatsUp Automation (com.whatsup.automation).
  Trigger on: "logcat", "سجلات", "لوجكات", "سجل الأحداث", "تتبع الخطأ", "كراش",
  "crash", "ANR", "تعليق التطبيق", "تجميد", "LMK", "lowmemorykiller", "قتل التطبيق",
  "تفريغ السجلات", "فحص السجل", "مراقبة الخدمة", "تتبع الجسر", "NodeRunner",
  "WhatsAppForegroundService", "BaileysBridge", "adb logcat", "pidof", "traces.txt",
  "fatal", "SIGSEGV", "SIGABRT", "OOM", "OutOfMemory", "diagnose crash".
---

# Android Logcat Analyzer & Runtime Diagnostics (WhatsUp Automation)

Specialized runtime log extraction and diagnostics protocol tailored for **WhatsUp Automation** (`com.whatsup.automation`), encompassing native C++/Node.js 18 layers, background Kotlin services, Room database transactions, and Compose UI lifecycles.

---

## 1. Quick Command Cheat Sheet

All commands are optimized to avoid terminal buffer overflows and maintain surgical precision.

### 1.1 Log Cleaning & Preparation
Always clear historical buffer logs before reproducing an issue:
```bash
adb logcat -c
```

### 1.2 Process-Specific Log Capture (PID Filter)
Filter logs strictly emitted by `com.whatsup.automation`:
- **Bash / Linux / macOS**:
  ```bash
  adb logcat --pid=$(adb shell pidof -s com.whatsup.automation)
  ```
- **Windows PowerShell**:
  ```powershell
  $pkg = "com.whatsup.automation"; $pid = (adb shell pidof -s $pkg).Trim(); if ($pid) { adb logcat --pid=$pid } else { Write-Warning "App is not running" }
  ```

### 1.3 Fatal Crash Dumps (Crash Buffer)
Instantly dump fatal uncaught Java/Kotlin exceptions and native tombstones without streaming background noise:
```bash
adb logcat -d -b crash
```
For crash logs with timestamps and thread information:
```bash
adb logcat -d -b crash -v time
```

### 1.4 Foreground Service & WakeLock Lifecycle Filters
Filter execution across service start, health polling, notification attachment, and WakeLock acquisitions:
```bash
adb logcat -s "WhatsAppForegroundService:*" "ServiceHealthMonitor:*" "NodeEngine:*" "BootReceiver:*"
```
*(Windows PowerShell syntax)*:
```powershell
adb logcat -s WhatsAppForegroundService:* ServiceHealthMonitor:* NodeEngine:* BootReceiver:*
```

### 1.5 Node.js Bridge & Native Execution Capture
Track embedded Node.js 18 engine, JNI layer (`NodeRunner`), socket IPC, and Baileys events:
```bash
adb logcat -s "NodeRunner:*" "BaileysBridge:*" "libc:*" "DEBUG:*"
```
Filter raw Baileys WhatsApp JSON events vs debug logs:
- **Bash**:
  ```bash
  adb logcat -s "BaileysBridge:*" | grep -E "(\[WHATSAPP_EVENT\]|Baileys Log)"
  ```
- **PowerShell**:
  ```powershell
  adb logcat -s BaileysBridge:* | Select-String -Pattern "(\[WHATSAPP_EVENT\]|Baileys Log)"
  ```

---

## 2. Systematic Crash Diagnostics Protocol

When diagnosing an error, adhere strictly to the 4-phase triage pipeline:

```
[Crash / Anomaly]
       │
       ▼
Phase 1: Fatal Extraction ───► adb logcat -d -b crash
       │
       ▼
Phase 2: Frame Pinpointing ──► Identify top frame in `com.whatsup.automation.*`
       │
       ▼
Phase 3: Targeted Source Read ─► Read suspect file lines (<= 50 lines)
       │
       ▼
Phase 4: Minimal Diff ────────► [file:line] ❌ broken -> ✅ fixed
```

### 2.1 Trace to Kotlin Source Line (Triage Steps)
1. **Extract Exception Header**: Locate the exact unhandled exception name:
   - Example: `FATAL EXCEPTION: main`, `java.lang.NullPointerException`, `android.app.ForegroundServiceStartNotAllowedException`.
2. **Find Root Cause Frame**: Ignore Android OS / Jetpack framework frames (`android.os.*`, `androidx.compose.*`). Trace down to the first frame within:
   ```
   com.whatsup.automation.<package>.<Class>.<method>(<File>.kt:<LineNumber>)
   ```
3. **Inspect Target Lines**: Use `read_file` targeting `StartLine` and `EndLine` (≤ 50 lines) around `<LineNumber>`.
4. **Determine Root Cause**: State the failure mechanism in exactly one sentence.
5. **Generate Surgical Diff**: Present fix formatted as:
   `[File.kt:LineNumber] ❌ oldCode -> ✅ newCode`

---

### 2.2 Catching ANR Traces (Application Not Responding)
An ANR occurs when the main UI thread is blocked for > 5 seconds (Input/Broadcast) or a Foreground Service does not call `startForeground()` within 10 seconds (Android 12+) or 200 seconds total.

1. **Detect ANR in Logcat**:
   ```bash
   adb logcat -d -b main -s "ActivityManager:E" "WindowManager:E" | grep "ANR in com.whatsup.automation"
   ```
2. **Fetch ANR Trace Dump**:
   - Modern Android (10+):
     ```bash
     # List recent ANR trace files
     adb shell "ls -t /data/anr/anr_*"
     # Pull the latest trace file
     adb pull /data/anr/<trace_file_name> ./anr_trace.txt
     ```
   - Legacy Android fallback:
     ```bash
     adb shell "cat /data/anr/traces.txt" > ./anr_trace.txt
     ```
3. **Analyze `main` Thread State in Trace**:
   - Locate section: `"main" prio=5 tid=1 ...`
   - Look for state markers:
     - `BLOCKED (on monitor ...)`: Thread is waiting on a synchronized block/Mutex held by background worker.
     - `TIMED_WAIT` / `WAITING`: Thread invoked synchronous blocking IO (`runBlocking`, `Thread.sleep`, socket read).
   - Trace callers to identify blocking calls executed on `Dispatchers.Main`.

---

### 2.3 Memory Pressure & Low Memory Killer (LMK) Detection
Android's `lowmemorykiller` terminates processes silently without triggering uncaught Java exception handlers.

1. **Detect Process Termination via Event Log**:
   ```bash
   adb logcat -d -b events -s "am_kill:*" | grep "com.whatsup.automation"
   ```
   *Interpretation*:
   - Reason `too many background processes` or `cached` → Process killed due to system RAM shortage.
   - Reason `empty` → Process had no active services/activities.

2. **Check Current OOM Score & Priority**:
   ```bash
   adb shell "cat /proc/$(adb shell pidof -s com.whatsup.automation)/oom_score_adj"
   ```
   - `0` to `100`: Protected Foreground Service (Lowest kill risk).
   - `200` to `700`: Visible activity / background service with high priority.
   - `900` to `1000`: Cached background process (First candidate for LMK termination).

3. **Inspect Real-Time Memory Consumption**:
   ```bash
   adb shell dumpsys meminfo com.whatsup.automation
   ```
   - Check `TOTAL PSS`: Memory footprint (Values > 250MB in background indicate leak in Node.js or Bitmap allocation).
   - Check `Native Heap`: Memory occupied by native C++ Node.js 18 runtime.
   - Check `Dalvik Heap`: Memory consumed by Kotlin/JVM Room objects and Compose states.

---

## 3. Evidence & Verification Protocol

1. **Zero Log Bloat Rule**:
   - Never print raw multi-page logcat logs directly in conversation outputs.
   - Extract and report solely the single actionable line:
     `[Logcat Evidence] <TAG>: <Exception or Log Message>`
2. **Deterministic Verification Cycle**:
   ```
   adb logcat -c  ──► Reproduce Action ──► adb logcat -d -b crash ──► Confirm Clean (0 fatal lines)
   ```
3. **Reporting Format**:
   - **Diagnostic**: Single sentence stating the root cause.
   - **Evidence**: `[TAG] com.whatsup.automation: <Exact Error>`
   - **Resolution**: `[File.kt:Line] ❌ <old> -> ✅ <new>`
