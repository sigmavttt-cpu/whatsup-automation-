---
name: strict-evidence-debug
description: >-
  Use this skill when fixing bugs, errors, crashes, build failures, or diagnosing unexpected behavior.
  Trigger on: "خطأ", "مشكلة", "كراش", "ما يشتغل", "ما يبني", "فشل البناء", "سبب الخطأ",
  "حل المشكلة", "لماذا يفشل", "لماذا", "ايش المشكلة", "شيل الخطأ", "ايش السبب",
  "مو شغال", "وقف", "انهار", "بيعطي خطأ", "مش شايل", "ما اشتغل",
  "fix", "bug", "error", "crash", "build failed", "exception", "stacktrace",
  "NullPointerException", "ClassCastException", "compilation error", "linker error",
  "ADB", "logcat", "Gradle failed", "why is it broken", "root cause", "debug".
---

# Strict Evidence-Based Debugging (Surgical-Grade)

## Mandatory Debug Protocol — 5 Steps, No Skipping

```
Step 1 — Locate:   Read the suspect file (StartLine/EndLine <= 50 lines). Read PROJECT_STATE.md first.
Step 2 — Diagnose: Identify the exact broken line + root cause in ONE sentence.
Step 3 — Diff:     [file:line] ❌ brokenCode → ✅ fixedCode  (1-2 lines only, no blocks)
Step 4 — Verify:   Run silent build. Report: ✅ Passed  OR  ❌ Failed: [one reason]
Step 5 — Sync:     Update PROJECT_STATE.md → Last Verified Changes section.
```

## Rules
- Zero guessing — if not read in this session, it is unknown.
- Two-Strike: fail twice → STOP, write 2-line root cause, wait for user.
- Never dump logs or stack traces in chat — extract the single relevant line only.
