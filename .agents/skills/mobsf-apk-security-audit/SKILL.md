---
name: mobsf-apk-security-audit
description: Guides the setup and execution of Mobile Security Framework (MobSF) and mobsfscan for static application security testing (SAST), correlating vulnerability findings to source code and eliminating false positives.
---

# MobSF and mobsfscan APK Security Audit Skill

This skill governs the execution of automated static application security testing (SAST) for Android source code and compiled APK binaries using `mobsfscan` and `MobSF`.

## 1. Tool Selection & Setup Rules

1. **mobsfscan (Source Code SAST):**
   - Lightweight, rule-based static scanner for Kotlin, Java, and AndroidManifest.xml.
   - Run in an isolated Python virtual environment (`.venv-security` or temp environment) without polluting global packages.
   - Command: `mobsfscan --json -o mobsfscan-report.json app/src/main`
2. **MobSF (Binary & Full Static Analysis):**
   - Full Mobile Security Framework container/service.
   - If Docker is available: `docker run -it --rm -p 8000:8000 opensecurity/mobsf:latest`
   - If Docker is unavailable: rely on `mobsfscan` for source code + Android SDK tools (`apksigner`, `apkanalyzer`, `aapt2`) for binary forensics.

## 2. Evidence Categorization Protocol

MobSF and SAST findings must be triaged rigorously into five categories:
- **Confirmed Vulnerability:** Verifiable security flaw exposing user data or allowing code execution (e.g. unencrypted sensitive storage, world-readable files).
- **Valid Hardening Improvement:** Best practice recommendation that strengthens security posture (e.g., removing unused high-privilege permissions, disabling legacy backup flags).
- **False Positive:** Scanner flagged a pattern out of context (e.g. loopback IPC labeled as "insecure cleartext communication").
- **Not Applicable:** Finding applies to an unused code branch or third-party test artifact.
- **Requires Further Investigation:** Finding with insufficient evidence requiring runtime verification or deep decompilation.

## 3. Scope Boundaries & Safety

- Never upload proprietary APKs or signing keys to public online multi-scanners without explicit developer approval.
- Bind all local web services strictly to `127.0.0.1` (localhost).
- Distinguish between theoretical security findings and actual causes of Google Play Protect flags.
