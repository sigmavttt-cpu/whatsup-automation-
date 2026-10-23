---
name: baileys-bridge-guard
description: >-
  Use this skill when inspecting, modifying, or debugging the Node.js WhatsApp bridge,
  Baileys library, status reactions, JID/LID routing, or any bridge-side JS logic.
  Trigger on: "حالات", "ستوري", "رياكشن", "بايليز", "نود", "جسر", "نود جي اس",
  "الجسر", "status@broadcast", "ارسل ستوري", "ستاتس", "ريآكشن على الحالة",
  "JID", "LID", "statusJidList", "normalizedParticipant", "مشكلة الجسر",
  "index.js", "nodejs-project", "ما يرسل الرياكشن", "ما تشتغل الحالات",
  "status", "baileys", "bridge", "reaction", "WhatsApp status", "story",
  "sendMessage", "WASocket", "makeWASocket", "socket", "disconnect", "reconnect",
  "QR", "pairing code", "session", "jid normalization", "LID routing".
---

# Baileys Dumb-Bridge & Single-JID Guard (Surgical-Grade)

## Mandatory Protocol

```
Step 1 — Locate:   Read only the relevant 50-line block in nodejs-project/*.js
Step 2 — Diagnose: Identify the exact issue in ONE sentence
Step 3 — Diff:     [file:line] ❌ old → ✅ new  (1-2 lines max)
Step 4 — Verify:   Confirm statusJidList = [normalizedParticipant] only, target = status@broadcast only
Step 5 — Sync:     Update PROJECT_STATE.md if change was made
```

## Hard Rules
- Dumb Bridge Law: zero autonomous logic in JS — all decisions come from Kotlin commands only.
- Single-JID: `statusJidList` = `[normalizedParticipant]` — never an array of multiple JIDs.
- Zero DM: reactions go to `status@broadcast` exclusively — no fallback to private chats.
- LID vs Phone: distinguish `@lid` from `@s.whatsapp.net` using `msg.key.participant` exact value.
