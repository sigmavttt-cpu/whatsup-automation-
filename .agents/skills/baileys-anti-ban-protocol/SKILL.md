---
name: baileys-anti-ban-protocol
description: Protects WhatsApp automation and Baileys sessions against account restrictions, bans, and rate-limiting. Covers human presence simulation, Gaussian jitter delays, gradual warming schedules, group metadata caching, silent socket lockup watchdogs, and 401/463 disconnect backoff. Trigger on "anti-ban", "حظر واتساب", "rate limit", "بان", "حظر الرقم", "تاخير الرسائل", "baileys ban", "spam detection", "presenceUpdate", "401 unauthorized", "463 timelock".
---

# Baileys WhatsApp Anti-Ban & Rate-Limiting Protocol

## 1. Context & Threat Model
WhatsApp detects bots using behavioral pattern analysis (heuristics), traffic velocity spikes, recipient interaction ratios (inbound vs. outbound), and abnormal socket behavior. Because Baileys simulates WhatsApp Web multi-device connections, automation MUST mimic human pacing and protocol subtleties.

---

## 2. Core Behavioral Rules

### I. Human Presence & Typing Simulation (Crucial)
Never send messages without prior presence states. A human reads, hesitates, and types:
```javascript
async function sendHumanMessage(sock, jid, text) {
    // 1. Subscribe to recipient presence
    await sock.presenceSubscribe(jid);
    
    // 2. Mark as read / seen (optional, depending on rule)
    await delay(getRandomInt(400, 1200));
    
    // 3. Send 'composing' presence (Typing indicator)
    await sock.sendPresenceUpdate('composing', jid);
    
    // 4. Calculate realistic typing delay (~30ms per character, min 1.5s, max 6s)
    const typingTime = Math.min(Math.max(text.length * 35, 1500), 6000);
    await delay(typingTime);
    
    // 5. Pause typing briefly
    await sock.sendPresenceUpdate('paused', jid);
    await delay(getRandomInt(300, 700));
    
    // 6. Send the actual payload
    return await sock.sendMessage(jid, { text });
}
```

### II. Gaussian Jitter (Non-Uniform Delays)
Fixed intervals (e.g. exactly 5000ms between every message) trigger algorithmic flags. Always add randomized Gaussian jitter:
```javascript
function getGaussianDelay(meanMs = 4000, stdDevMs = 1200) {
    let u = 0, v = 0;
    while (u === 0) u = Math.random();
    while (v === 0) v = Math.random();
    let num = Math.sqrt(-2.0 * Math.log(u)) * Math.cos(2.0 * Math.PI * v);
    let result = meanMs + num * stdDevMs;
    return Math.max(Math.round(result), 1500); // Enforce min 1.5s
}
```

### III. Group Participant Caching (Prevent Server Spam)
When sending messages or reactions to groups, Baileys encrypts the payload for every participant. Without caching, it queries WhatsApp servers for group metadata on EVERY single message, causing instant rate-limiting.
```javascript
const groupCache = new Map();

const sock = makeWASocket({
    auth: state,
    cachedGroupMetadata: async (jid) => {
        if (groupCache.has(jid)) return groupCache.get(jid);
        const metadata = await sock.groupMetadata(jid);
        groupCache.set(jid, metadata);
        return metadata;
    }
});
```

### IV. Number Warming Schedule (For New or Re-paired Numbers)
| Phase | Duration | Max Outbound / Day | Recommended Behavior |
| :--- | :--- | :---: | :--- |
| **Stage 1 (Warmup)** | Days 1 - 3 | 15 - 30 messages | Only reply to incoming messages; zero bulk broadcast. |
| **Stage 2 (Expansion)** | Days 4 - 7 | 50 - 100 messages | Spread evenly across active hours (9 AM - 10 PM). |
| **Stage 3 (Production)**| Day 8+ | 200 - 400 messages | Never exceed 15 msgs/minute. Maintain 1:1 inbound/outbound ratio. |

---

## 3. Deaf Session Watchdog (Mutex Lockup Prevention)
Baileys instances can experience silent lockups where the WebSocket stays open and pings succeed, but `messages.upsert` stops firing because of internal crypto queue deadlocks.

**The Watchdog Solution:**
```javascript
let lastActivityTimestamp = Date.now();

// Reset on any incoming frame
sock.ev.on('messages.upsert', () => { lastActivityTimestamp = Date.now(); });
sock.ev.on('connection.update', () => { lastActivityTimestamp = Date.now(); });

// Periodic health check (every 3 minutes)
setInterval(() => {
    const elapsed = Date.now() - lastActivityTimestamp;
    if (sock.ws?.isOpen && elapsed > 15 * 60 * 1000) { // 15 min total silence
        console.warn('[Watchdog] Deaf session detected. Initiating clean reconnect.');
        sock.end(new Error('DeafSessionWatchdogTimeout'));
    }
}, 3 * 60 * 1000);
```

---

## 4. Error Code Handling & Circuit Breaker
- **`401 Unauthorized` / `loggedOut`:**
  - Hard stop immediately.
  - Do NOT retry in a loop.
  - Signal Kotlin to notify the user to re-pair via QR or Pairing Code.
- **`403 Forbidden` / `463 Timelock`:**
  - WhatsApp has temporarily blocked sending.
  - Back off for at least 15 to 30 minutes. Pause the outbox queue completely.
- **`515 Restart Required`:**
  - Expected after initial multi-device pairing.
  - Wait 3 seconds, tear down previous socket cleanly, and reinitialize.
