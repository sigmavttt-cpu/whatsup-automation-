---
name: android-release-integrity
description: Enforces production release integrity for Android APKs, verifying signature schemes (v1, v2, v3, v4), certificate metadata, merged manifest configuration, debuggable flags, and signing key preservation.
---

# Android Release Integrity Skill

This skill governs the verification and configuration of Android release builds to ensure that binaries distributed to users meet strict release criteria and avoid trigger patterns that security scanners flag.

## 1. Release Configuration Checklist

A genuine production release build must satisfy:
1. **`buildType` = `release`:**
   - `isDebuggable = false` (never distribute APKs with `android:debuggable="true"`).
   - Code shrinking and resource optimization enabled (`isMinifyEnabled = true`, `isShrinkResources = true`).
   - Obfuscation and keep rules defined in `proguard-rules.pro`.
2. **Signature Schemes (Full Multi-Scheme Signing):**
   - **V1 (JAR Signing):** Required for legacy Android versions (< 7.0).
   - **V2 (Full APK Signature Scheme):** Required for Android 7.0+. Protects entire APK binary.
   - **V3 (APK Signature Scheme v3):** Required for Android 9.0+. Supports key rotation proof.
   - **V4 (APK Signature Scheme v4):** Generates `.idsig` for streaming installation on Android 11+.
3. **Preservation of Signing Identity:**
   - The signing certificate fingerprint (SHA-256) must remain identical across all updates to allow in-place updates.
   - Never rotate, replace, or regenerate the release keystore without explicit approval.

## 2. Verification Protocol

Always execute the official Android SDK `apksigner` command to verify the built release APK:

```bash
apksigner verify --verbose --print-certs <path-to-apk>
```

Verify that:
- `Verifies` returns `true`.
- Signer #1 DN represents the authentic organization identity (not `CN=Android Debug`).
- Signer #1 SHA-256 matches the authorized project certificate.
- Schemes v1, v2, v3 verify as `true`.
