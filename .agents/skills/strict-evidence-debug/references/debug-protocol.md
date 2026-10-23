# Debug Protocol — Full Reference

## 5-Step Surgical Debug Sequence
1. **Locate:** Read suspect file targeted (StartLine/EndLine ≤ 50 lines). Check PROJECT_STATE.md first.
2. **Diagnose:** One sentence — exact broken line + root cause.
3. **Diff:** `[file:line] ❌ broken → ✅ fixed` — max 2 lines shown in chat.
4. **Verify:** Silent build. Result: `✅ Passed` OR `❌ Failed: [one reason]`.
5. **Sync:** Update `PROJECT_STATE.md → Last Verified Changes`.

## Two-Strike Rule
- First failure: try one minimal alternative fix.
- Second failure: STOP. Write 2-line root cause. Wait for user direction.

## Error Triage Matrix
| Error Type | First File to Read | Key Identifier |
|:---|:---|:---|
| Kotlin compile error | `app/src/main/kotlin/**/*.kt` | Line number in error |
| Node.js bridge error | `nodejs-project/index.js` | Error stack first line |
| Room DB migration | `AppDatabase.kt` | Migration version number |
| Notification/Reply | `WhatsAppNotificationListenerService.kt` | Cache key |
| Status reaction | `status-manager.js` | `reactStatus` function |
