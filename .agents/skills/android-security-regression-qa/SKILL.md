---
name: android-security-regression-qa
description: Establishes security baselines, manages regression test suites for Android security fixes, and ensures that remediation of warnings does not break core messaging, background services, status handling, or database integrity.
---

# Android Security Regression QA Skill

This skill governs the systematic regression verification of Android applications during and after security hardening or Play Protect remediation.

## 1. Core Workflow Protection Matrix

Before and after applying any security correction (e.g. permission removal, manifest adjustments, code shrinking tweaks), the following critical capabilities must be explicitly verified:

1. **Messaging & Node.js Engine:**
   - JNI socket communication on `127.0.0.1:6789`.
   - Node.js runtime initialization and Baileys socket handshake.
   - Incoming and outgoing message dispatching.
2. **Status Automation & Media Handling:**
   - Status story reception (`INCOMING_STATUS`).
   - Ghost mode & quoted reaction simulation.
   - Status media download and saving to Pictures vault via MediaStore / Scoped Storage.
3. **Background Persistence & System Services:**
   - `WhatsAppForegroundService` lifecycle with remote messaging type.
   - `WhatsAppNotificationListenerService` capturing and deduplication (`processedMessageCache`).
   - `BootReceiver` restart triggers.
4. **Data & Keystore Integrity:**
   - Room database operations (`app_database.db`) and migrations.
   - Android KeyStore encryption/decryption of session tokens (`SessionKeystore`).

## 2. Test Execution Commands

- **Unit Tests:**
  `.\gradlew testDebugUnitTest -q *> unit_tests.log`
- **Node.js Engine Suite:**
  `node app/src/main/assets/nodejs-project/test_modules.js`
  `node app/src/main/assets/nodejs-project/test_adversarial_status.js`
- **Release Compilation & Packaging:**
  `.\gradlew assembleRelease -q *> build_release.log`
