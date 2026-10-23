---
name: android-play-protect-triage
description: Classifies Android installation and Google Play Protect warnings, consults official Google PHA policies, distinguishes unverified-developer prompts from harmful app blocks, and guides evidence-backed false-positive appeals.
---

# Android Play Protect Triage Skill

This skill provides a systematic protocol for diagnosing, categorizing, and remediating Google Play Protect warnings for sideloaded and independently distributed Android applications.

## 1. Classification Matrix

When an Android application displays an installation warning via Google Play Protect, classify the event into one of the following distinct categories:

| Category | Typical UI Display | Primary Cause | Legitimate Remediation Path |
| :--- | :--- | :--- | :--- |
| **A. Unrecognized Developer** *(Soft Prompt)* | "Play Protect doesn't recognize this app's developer" with "Install anyway" behind "More details" | APK signed with a new or private keystore with zero global install telemetry; not listed in Google Play Store. | Complete keystore metadata; maintain consistent signing; file Play Protect Developer Form or seed via Play Console Internal Testing track. |
| **B. Potentially Harmful Application (PHA)** *(Hard Block)* | Red/Orange modal: "Blocked by Play Protect" / "Harmful app detected" / "Unsafe app blocked" | Heuristic or signature match on high-risk permissions (`REQUEST_INSTALL_PACKAGES`, `MANAGE_EXTERNAL_STORAGE`, `ACCESSIBILITY`), suspected brand impersonation, dropper behavior, or dynamic code loading. | Remove unnecessary dangerous permissions; eliminate dropper/DCL patterns; align with Google Unwanted Software Policy; submit official false-positive appeal. |
| **C. Brand Impersonation / Phishing** | "This app is fake or poses a risk to personal data" | Package name, app label, or UI mimics a major application (e.g. WhatsApp, banks) while requesting sensitive data. | Clarify application identity; ensure distinct branding and disclosure; explain automation use case in appeal. |
| **D. Obsolete Target SDK Block** | "This app was built for an older version of Android" | `targetSdkVersion` below Google's required floor for Android version (e.g., `< 34` on Android 14+). | Upgrade `targetSdkVersion` to modern Android API standards (API 34/35). |
| **E. Debug Keystore Flag** | "App is signed with a debug certificate" | APK signed with standard `CN=Android Debug` key (`debug.keystore`). | Must never distribute debug APKs; always sign with official production `release.keystore`. |

## 2. Investigation Checklist

1. **Verify Artifact Origin:**
   - Check variant (`debug` vs `release`).
   - Check `android:debuggable` flag in merged manifest.
2. **Inspect Certificate Identity:**
   - Execute `apksigner verify --verbose --print-certs <apk-path>`.
   - Record SHA-256 certificate fingerprint and Issuer/Subject DN.
3. **Audit Dangerous Permissions:**
   - Inspect manifest for `REQUEST_INSTALL_PACKAGES`, `MANAGE_EXTERNAL_STORAGE`, `BIND_NOTIFICATION_LISTENER_SERVICE`, `READ_CONTACTS`, etc.
   - Trace if each permission is actively utilized in code or merely redundant.
4. **Examine Embedded Binaries & Dynamic Code Loading:**
   - Check native libraries (`.so` files) and bundled assets (Node.js/JS scripts, dex files).
   - Verify all execution paths operate locally on loopback (`127.0.0.1`) without remote payload downloading.

## 3. Official Google Appeal Protocol

If the application is a legitimate local utility falsely flagged as PHA:
1. Ensure the APK is a clean production release build signed with the official key.
2. Obtain a direct raw download link to the APK (e.g. GitHub Releases asset URL).
3. Compute exact APK SHA-256 file hash and signing certificate SHA-256 fingerprint.
4. Prepare a factual justification:
   - Outline the application's legitimate productivity function.
   - Disclose offline-only / zero-telemetry architecture.
   - Detail the exact operational necessity of permissions (e.g., notification listener for auto-reply).
5. Submit appeal via official portal:
   `https://support.google.com/googleplay/android-developer/contact/protectappeals`
