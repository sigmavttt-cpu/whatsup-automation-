# JID Routing Protocol — Full Reference

## Single-JID Validation Checklist
- [ ] `statusJidList` = `[normalizedParticipant]` — never more than one JID.
- [ ] `normalizedParticipant` = `jidNormalizedUser(msg.key.participant)`.
- [ ] Target = `status@broadcast` — never the participant JID directly.
- [ ] LID (`@lid`) → reply to LID only. Phone (`@s.whatsapp.net`) → reply to phone only.
- [ ] Zero DM fallback — no `sendMessage` to private chat under any condition.

## Dumb Bridge Validation Checklist
- [ ] No autonomous `if` conditions in JS that trigger without Kotlin command.
- [ ] No cron/timer logic in JS — scheduling belongs to Kotlin `AlarmManager`.
- [ ] No direct DB reads from JS — data flows through Kotlin → socket → JS only.

## Key Functions Map
| Function | File | Purpose |
|:---|:---|:---|
| `reactStatus` | `status-manager.js:314` | Sends emoji reaction to status |
| `viewStatus` | `status-manager.js:240` | Marks status as viewed |
| `dispatchCommand` | `command-dispatcher.js` | Routes all Kotlin commands |
