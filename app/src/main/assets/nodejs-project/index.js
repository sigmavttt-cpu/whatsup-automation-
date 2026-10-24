/**
 * WhatsApp Automation Engine — Local Baileys Bridge
 * Runs locally on the Android device with zero external servers.
 * Modular entry point orchestrating lifecycle and event bindings.
 */

import makeWASocket, {
    DisconnectReason,
    useMultiFileAuthState,
    fetchLatestBaileysVersion,
    downloadMediaMessage,
    downloadContentFromMessage,
    normalizeMessageContent,
    extractMessageContent,
    getContentType,
    makeCacheableSignalKeyStore
} from '@whiskeysockets/baileys';
import {
    CONFIG,
    logger,
    sentMessagesCache,
    rawMessagesCache,
    msgRetryCounterCache
} from './config.js';
import { initBridge, emitToAndroid } from './bridge.js';
import {
    loadLidPhoneMap,
    saveLidPhoneMap,
    linkLidAndPhone,
    resolvePhoneAndLid,
    isLidJidOrNumber,
    setContactName,
    getContactName
} from './lid-resolver.js';
import {
    loadStatusKeys,
    processStatusMessage,
    getStatusKeysCount
} from './status-manager.js';
import { attachSocketTraceHook } from './tracer.js';
import { createCommandDispatcher } from './command-dispatcher.js';

import fs from 'fs';
import path from 'path';

let sock = null;
let isStarting = false;
let currentAuthFolder = process.argv[2] || CONFIG.DEFAULT_AUTH_FOLDER;
let heartbeatInterval = null;

/**
 * Self-healing: Safely logs Signal session errors without destroying all unrelated sessions.
 */
function healBadMacSession(err) {
    try {
        const errStr = String(err?.stack || err?.message || err || '');
        if (errStr.includes('Bad MAC')) {
            console.warn('[Self-Healing] Transient Bad MAC detected in Signal ratchet; relying on Baileys PreKey retry resolver.');
        }
    } catch (_) {}
}

process.on('uncaughtException', (err) => {
    console.error('Node uncaughtException:', err?.stack || err);
    healBadMacSession(err);
});
process.on('unhandledRejection', (reason) => {
    console.error('Node unhandledRejection:', reason);
    healBadMacSession(reason);
});
if (process.stdin) {
    process.stdin.on('error', () => {
        // Ignore stdin stream errors on Android
    });
}

// Keep Node.js event loop alive 24/7
setInterval(() => {}, 60000);

/**
 * Processes batches of contacts received during history sync or contact updates.
 */
async function handleContactsBatch(contactsList, authFolder = currentAuthFolder) {
    if (!contactsList || !Array.isArray(contactsList)) return;
    const validContacts = [];

    for (const c of contactsList) {
        if (!c || !c.id) continue;

        const savedName = c.name || c.verifiedName || c.notify || '';
        const rawId = String(c.id);
        const lid = c.lid ? String(c.lid) : (rawId.includes('@lid') ? rawId : null);
        const phone = c.phoneNumber || c.pn || (rawId.includes('@s.whatsapp.net') ? rawId : null);

        if (lid && phone) {
            linkLidAndPhone(lid, phone, savedName, authFolder);
        }

        const resolved = await resolvePhoneAndLid(c.id, null, sock);
        const cleanPhone = (resolved.realPhone || resolved.phone || '').replace(/[^0-9]/g, '');

        if (cleanPhone && cleanPhone.length >= 7 && !isLidJidOrNumber(cleanPhone) && savedName && savedName.trim().length >= 2) {
            const trimmed = savedName.trim();
            const isNumeric = trimmed.replace(/[^0-9]/g, '').length === trimmed.length;
            if (!isNumeric && !trimmed.includes('@') && !trimmed.includes('lid')) {
                validContacts.push({
                    phone: cleanPhone,
                    name: trimmed
                });
                setContactName(cleanPhone, trimmed);
            }
        }
    }

    if (validContacts.length > 0) {
        emitToAndroid('WHATSAPP_CONTACTS_SYNC', { contacts: validContacts });
        saveLidPhoneMap(authFolder);
    }
}

/**
 * ينهي المقبس القديم بشكل جذري ويفرغ مستمعي الأحداث لمنع المقابس الشبحية وتسرب الذاكرة.
 */
function terminateExistingSocket() {
    if (heartbeatInterval) {
        clearInterval(heartbeatInterval);
        heartbeatInterval = null;
    }
    if (sock) {
        try {
            if (sock.ev && typeof sock.ev.removeAllListeners === 'function') {
                sock.ev.removeAllListeners();
            }
        } catch (_) {}
        try {
            if (sock.ws && typeof sock.ws.close === 'function') {
                sock.ws.close();
            }
        } catch (_) {}
        try {
            if (typeof sock.end === 'function') {
                sock.end();
            }
        } catch (_) {}
        sock = null;
    }
}

/**
 * Starts or restarts the Baileys WhatsApp session.
 */
export async function startWhatsAppSession(authFolder = currentAuthFolder) {
    if (isStarting) return;
    isStarting = true;

    terminateExistingSocket();

    loadStatusKeys(authFolder);
    loadLidPhoneMap(authFolder);

    const { state, saveCreds } = await useMultiFileAuthState(authFolder);
    const { version } = await fetchLatestBaileysVersion();

    sock = makeWASocket({
        version,
        auth: {
            creds: state.creds,
            keys: makeCacheableSignalKeyStore(state.keys, logger)
        },
        logger,
        printQRInTerminal: false,
        browser: CONFIG.BROWSER_CONFIG,
        syncFullHistory: true,
        markOnlineOnConnect: true,
        generateHighQualityLinkPreview: false,
        msgRetryCounterCache,
        // PreKey Retry Resolver: answers decryption retries from cache to prevent "Waiting for this message"
        getMessage: async (key) => {
            if (!key?.id) return undefined;
            return sentMessagesCache.get(key.id) || rawMessagesCache.get(key.id) || undefined;
        }
    });

    attachSocketTraceHook(sock);

    sock.ev.on('creds.update', async () => {
        try {
            await saveCreds();
            const credsFile = path.join(authFolder, 'creds.json');
            if (fs.existsSync(credsFile) && fs.statSync(credsFile).size > 50 && sock?.user) {
                const userPhone = sock.user.id ? String(sock.user.id).replace(/[^0-9]/g, '').substring(0, 15) : '';
                emitToAndroid('SESSION_PERSISTED', {
                    phone: userPhone,
                    verified: true
                });
            }
        } catch (e) {
            console.error('[creds.update] Error saving and verifying creds:', e);
        }
    });

    sock.ev.on('connection.update', async (update) => {
        const { connection, lastDisconnect, qr } = update;

        if (qr) {
            emitToAndroid('QR_CODE', { qr });
        }

        if (connection === 'close') {
            if (heartbeatInterval) {
                clearInterval(heartbeatInterval);
                heartbeatInterval = null;
            }

            const statusCode = (lastDisconnect?.error)?.output?.statusCode;
            const isLoggedOut = statusCode === DisconnectReason.loggedOut || statusCode === 401 || statusCode === 403;
            const shouldReconnect = !isLoggedOut;

            isStarting = false;

            if (isLoggedOut) {
                try {
                    if (fs.existsSync(authFolder)) {
                        fs.rmSync(authFolder, { recursive: true, force: true });
                    }
                } catch (err) {
                    console.error('Failed to clean authFolder on logout:', err);
                }
                emitToAndroid('LOGGED_OUT', { 
                    reason: 'تم تسجيل الخروج من الأجهزة المرتبطة في تطبيق واتساب' 
                });
            }

            // فحص ما إذا كان الانقطاع حدث أثناء مرحلة ربط الجهاز وقبل المصادقة
            const isPairingPhase = !sock?.authState?.creds?.registered && !sock?.user;
            if (isPairingPhase && lastDisconnect?.error) {
                let pairingReason = 'فشلت عملية الربط مع تطبيق واتساب.';
                if (statusCode === 401 || statusCode === 403) {
                    pairingReason = 'تم رفض كود الاقتران أو إلغاء ربط الجهاز من تطبيق واتساب.';
                } else if (statusCode === 408 || statusCode === 428) {
                    pairingReason = 'انتهت مهلة ربط الجهاز أو انقطع الاتصال أثناء المصادقة مع واتساب.';
                } else if (statusCode === 515) {
                    pairingReason = 'مطلوب إعادة تشغيل المحرك لاستكمال الاقتران بنجاح.';
                } else if (lastDisconnect.error.message) {
                    pairingReason = `تعذر إتمام الاقتران: ${lastDisconnect.error.message}`;
                }
                emitToAndroid('PAIRING_FAILED', {
                    reason: pairingReason,
                    statusCode: statusCode || 0
                });
            }

            emitToAndroid('CONNECTION_STATE', {
                status: 'DISCONNECTED',
                reconnecting: shouldReconnect,
                reason: isLoggedOut 
                    ? 'تم تسجيل الخروج من واتساب' 
                    : (lastDisconnect?.error?.message || 'closed')
            });

            if (shouldReconnect) {
                setTimeout(() => startWhatsAppSession(authFolder), 3000);
            }
        } else if (connection === 'open') {
            isStarting = false;
            const user = sock.user;

            try {
                await sock.sendPresenceUpdate('available');
            } catch (_) {}

            // فحص وتأكيد كتابة ملف creds.json المشفر بالقرص
            let isCredsVerified = false;
            try {
                const credsFile = path.join(authFolder, 'creds.json');
                if (fs.existsSync(credsFile) && fs.statSync(credsFile).size > 50) {
                    isCredsVerified = true;
                }
            } catch (_) {}

            // مستشعر النبض الحي (Heartbeat Pinger) لفحص حيوية المقبس كل 45 ثانية وكشف السقوط الصامت
            if (heartbeatInterval) clearInterval(heartbeatInterval);
            heartbeatInterval = setInterval(async () => {
                if (sock && sock.user) {
                    try {
                        await sock.sendPresenceUpdate('available');
                    } catch (err) {
                        console.warn('[Heartbeat Pinger] Silent socket drop detected:', err?.message);
                        if (heartbeatInterval) {
                            clearInterval(heartbeatInterval);
                            heartbeatInterval = null;
                        }
                        emitToAndroid('CONNECTION_STATE', {
                            status: 'DISCONNECTED',
                            reconnecting: true,
                            reason: 'انقطع اتصال المقبس بصمت (Silent Socket Drop)'
                        });
                        terminateExistingSocket();
                        setTimeout(() => startWhatsAppSession(authFolder), 3000);
                    }
                }
            }, 45000);

            emitToAndroid('CONNECTION_STATE', {
                status: 'CONNECTED',
                user: {
                    id: user?.id,
                    name: user?.name || user?.notify || ''
                },
                sessionVerified: isCredsVerified
            });

            if (isCredsVerified && user?.id) {
                const cleanPhone = String(user.id).replace(/[^0-9]/g, '').substring(0, 15);
                emitToAndroid('SESSION_PERSISTED', {
                    phone: cleanPhone,
                    verified: true
                });
            }

            const count = getStatusKeysCount();
            emitToAndroid('STATUS_FETCH_DONE', {
                count,
                message: `جاهز لاستقبال ومعالجة الحالات (المفاتيح المخزنة: ${count})`
            });
        }
    });

    // History sync listener
    sock.ev.on('messaging-history.set', async ({ chats, contacts, messages }) => {
        if (chats && Array.isArray(chats)) {
            for (const chat of chats) {
                if (chat.id && chat.lidJid) {
                    linkLidAndPhone(chat.lidJid, chat.id, chat.name, authFolder);
                }
                if (chat.pnJid && chat.lidJid) {
                    linkLidAndPhone(chat.lidJid, chat.pnJid, chat.name, authFolder);
                }
            }
        }
        if (contacts && contacts.length > 0) {
            await handleContactsBatch(contacts, authFolder);
        }
        if (messages && messages.length > 0) {
            for (const msg of messages) {
                if (msg.key?.remoteJid === 'status@broadcast') {
                    await processStatusMessage(msg, authFolder, sock);
                }
            }
        }
    });

    // Contacts upsert listener
    sock.ev.on('contacts.upsert', async (contacts) => {
        await handleContactsBatch(contacts, authFolder);
    });

    // Chats upsert listener for immediate LID/phone linking
    sock.ev.on('chats.upsert', (chats) => {
        if (chats && Array.isArray(chats)) {
            for (const chat of chats) {
                if (chat.id && chat.lidJid) {
                    linkLidAndPhone(chat.lidJid, chat.id, chat.name, authFolder);
                }
                if (chat.pnJid && chat.lidJid) {
                    linkLidAndPhone(chat.lidJid, chat.pnJid, chat.name, authFolder);
                }
                if (chat.id && chat.id.includes('@lid') && chat.pnJid) {
                    linkLidAndPhone(chat.id, chat.pnJid, chat.name, authFolder);
                }
            }
        }
    });

    // Chats update listener
    sock.ev.on('chats.update', (updates) => {
        if (updates && Array.isArray(updates)) {
            for (const chat of updates) {
                if (chat.id && chat.lidJid) {
                    linkLidAndPhone(chat.lidJid, chat.id, chat.name, authFolder);
                }
                if (chat.pnJid && chat.lidJid) {
                    linkLidAndPhone(chat.lidJid, chat.pnJid, chat.name, authFolder);
                }
            }
        }
    });

    // Contacts update listener
    sock.ev.on('contacts.update', async (updates) => {
        if (!updates || !Array.isArray(updates)) return;
        for (const u of updates) {
            if (!u || !u.id) continue;
            const savedName = u.name || u.verifiedName || u.notify || '';
            const lid = u.lid ? String(u.lid) : (String(u.id).includes('@lid') ? String(u.id) : null);
            const phone = u.phoneNumber || u.pn || (String(u.id).includes('@s.whatsapp.net') ? String(u.id) : null);

            if (lid && phone) {
                linkLidAndPhone(lid, phone, savedName, authFolder);
            }

            const resolved = await resolvePhoneAndLid(u.id, null, sock);
            const cleanPhone = (resolved.realPhone || resolved.phone || '').replace(/[^0-9]/g, '');

            if (cleanPhone && cleanPhone.length >= 7 && !isLidJidOrNumber(cleanPhone) && savedName && savedName.trim().length >= 2) {
                const trimmed = savedName.trim();
                const isNumeric = trimmed.replace(/[^0-9]/g, '').length === trimmed.length;
                if (!isNumeric && !trimmed.includes('@')) {
                    setContactName(cleanPhone, trimmed);
                    emitToAndroid('WHATSAPP_CONTACT_UPDATED', {
                        phone: cleanPhone,
                        name: trimmed
                    });
                }
            }
        }
    });

/**
 * فك التغليف للرسائل المركبة (ephemeralMessage, documentWithCaptionMessage, deviceSentMessage)
 */
function unwrapMessage(rawMsg) {
    let current = rawMsg;
    let depth = 0;
    while (current && depth < 10) {
        depth++;
        if (current.ephemeralMessage?.message) {
            current = current.ephemeralMessage.message;
            continue;
        }
        if (current.documentWithCaptionMessage?.message) {
            current = current.documentWithCaptionMessage.message;
            continue;
        }
        if (current.deviceSentMessage?.message) {
            current = current.deviceSentMessage.message;
            continue;
        }
        break;
    }

    const extracted = (typeof extractMessageContent === 'function' ? extractMessageContent(current) : null)
                      || (typeof normalizeMessageContent === 'function' ? normalizeMessageContent(current) : null)
                      || current;

    return extracted;
}

    // Messages listener: processes incoming status stories and regular chat messages
    sock.ev.on('messages.upsert', async ({ messages, type }) => {
        for (const msg of messages) {
            try {
                const remoteJid = msg.key?.remoteJid || '';
                if (remoteJid.endsWith('@newsletter') || (remoteJid.includes('@broadcast') && remoteJid !== 'status@broadcast')) continue;

                const isGroup = remoteJid.endsWith('@g.us');
                const actualSender = isGroup ? (msg.key?.participant || '') : remoteJid;
                const cleanRawDigits = actualSender.split('@')[0].split(':')[0].replace(/[^0-9]/g, '');

                // Status stories
                if (remoteJid === 'status@broadcast') {
                    if (msg.key?.fromMe || !msg.message) continue;
                    await processStatusMessage(msg, authFolder, sock);
                    continue;
                }

                if (!msg.message) continue;
                if (msg.key?.fromMe) continue;

                // فك التغليف واستخراج المحتوى الداخلي
                const leaf = unwrapMessage(msg.message);
                const contentType = typeof getContentType === 'function' ? getContentType(leaf) : null;

                const mediaObj = leaf?.imageMessage || 
                                 leaf?.videoMessage || 
                                 leaf?.audioMessage || 
                                 leaf?.documentMessage;

                const isImage = !!(leaf?.imageMessage || contentType === 'imageMessage');
                const isVideo = !!(leaf?.videoMessage || contentType === 'videoMessage');
                const isAudio = !!(leaf?.audioMessage || contentType === 'audioMessage');
                const isDoc = !!(leaf?.documentMessage || contentType === 'documentMessage');
                const hasMedia = !!mediaObj || isImage || isVideo || isAudio || isDoc;

                const mediaType = isImage ? 'IMAGE' : (isVideo ? 'VIDEO' : (isAudio ? 'AUDIO' : (isDoc ? 'DOCUMENT' : 'NONE')));

                // ربط جهات الاتصال وحل أرقام LID
                if (msg.key?.senderPn) {
                    linkLidAndPhone(actualSender, msg.key.senderPn, msg.pushName, authFolder);
                }
                if (msg.key?.participantPn) {
                    linkLidAndPhone(actualSender, msg.key.participantPn, msg.pushName, authFolder);
                }

                let resolved = { phone: cleanRawDigits, realPhone: null, lid: null, isLid: false };
                try {
                    resolved = await resolvePhoneAndLid(actualSender, msg, sock);
                } catch (_) {}
                const cleanPhone = resolved.realPhone || resolved.phone || cleanRawDigits;

                if (msg.pushName && cleanPhone) {
                    const trimmedPush = msg.pushName.trim();
                    setContactName(cleanPhone, trimmedPush);
                    if (resolved.lid) {
                        setContactName(resolved.lid, trimmedPush);
                    }
                }

                const protocolMsg = msg.message?.protocolMessage;

                // 1. كاشف حذف الرسائل لدى الجميع (Anti-Delete Revocation)
                if (protocolMsg && protocolMsg.type === 0 && protocolMsg.key?.id) {
                    const revokedId = protocolMsg.key.id;
                    const cached = rawMessagesCache.get(revokedId);
                    const originalText = cached?.text || (typeof cached === 'string' ? cached : '');

                    emitToAndroid('MESSAGE_REVOKED', {
                        id: revokedId,
                        senderPhone: cleanPhone || actualSender,
                        senderName: msg.pushName || getContactName(cleanPhone) || '',
                        originalText: originalText,
                        timestamp: Date.now()
                    });
                    continue;
                }

                // 2. كاشف تعديل الرسائل وحفظ النص السابق (Anti-Edit Message Revision)
                if (protocolMsg && protocolMsg.type === 14 && protocolMsg.key?.id) {
                    const editedId = protocolMsg.key.id;
                    const cached = rawMessagesCache.get(editedId);
                    const previousText = cached?.text || (typeof cached === 'string' ? cached : 'نص غير مسجل مسبقاً');

                    const editedLeaf = (typeof extractMessageContent === 'function' ? extractMessageContent(protocolMsg.editedMessage) : null) || protocolMsg.editedMessage;
                    const newText = editedLeaf?.conversation ||
                                    editedLeaf?.extendedTextMessage?.text ||
                                    editedLeaf?.imageMessage?.caption ||
                                    editedLeaf?.videoMessage?.caption || '';

                    if (newText) {
                        emitToAndroid('MESSAGE_EDITED', {
                            id: `${editedId}_edit_${Date.now()}`,
                            originalId: editedId,
                            senderPhone: cleanPhone || actualSender,
                            senderName: msg.pushName || getContactName(cleanPhone) || '',
                            previousText: previousText,
                            newText: newText,
                            timestamp: Date.now()
                        });

                        // تحديث الكاش بالنص الجديد لأي تعديل لاحق
                        rawMessagesCache.set(editedId, {
                            text: newText,
                            senderPhone: cleanPhone || actualSender,
                            senderName: msg.pushName || getContactName(cleanPhone) || '',
                            timestamp: Date.now()
                        });
                    }
                    continue;
                }

                // 3. استخراج نص الرسالة العادية أو وصف الوسائط
                let text = leaf?.conversation ||
                           leaf?.extendedTextMessage?.text ||
                           leaf?.imageMessage?.caption ||
                           leaf?.videoMessage?.caption ||
                           leaf?.documentMessage?.caption ||
                           mediaObj?.caption ||
                           '';

                if (!text && hasMedia) {
                    if (isImage) text = '📷 [صورة]';
                    else if (isVideo) text = '🎥 [فيديو]';
                    else if (isAudio) text = '🎙️ [رسالة صوتية]';
                    else if (isDoc) text = '📄 [مستند]';
                }

                if (!text || !cleanPhone) continue;

                const senderName = msg.pushName || getContactName(cleanPhone) || '';

                // حفظ في الكاش لاسترجاع النص فور الحذف أو التعديل
                if (msg.key?.id) {
                    rawMessagesCache.set(msg.key.id, {
                        text,
                        senderPhone: cleanPhone,
                        senderName,
                        timestamp: msg.messageTimestamp ? Number(msg.messageTimestamp) * 1000 : Date.now()
                    });
                    if (rawMessagesCache.size > CONFIG.MAX_RAW_MESSAGES) {
                        const firstKey = rawMessagesCache.keys().next().value;
                        rawMessagesCache.delete(firstKey);
                    }
                }

                emitToAndroid('INCOMING_MESSAGE', {
                    id: msg.key.id,
                    senderPhone: cleanPhone,
                    realPhone: resolved.realPhone,
                    lid: resolved.lid,
                    isLid: resolved.isLid,
                    senderName,
                    text,
                    isGroup,
                    isViewOnce: false,
                    hasMedia,
                    mediaType,
                    timestamp: msg.messageTimestamp ? Number(msg.messageTimestamp) * 1000 : Date.now()
                });
            } catch (msgErr) {
                console.error('[MESSAGES_UPSERT] Unexpected error processing message:', msgErr);
            }
        }
    });
}

// Initialize command dispatcher and bridge
const commandDispatcher = createCommandDispatcher({
    getSocket: () => sock,
    startSession: (folder) => startWhatsAppSession(folder),
    getAuthFolder: () => currentAuthFolder,
    setAuthFolder: (folder) => { currentAuthFolder = folder; }
});

initBridge(commandDispatcher);

// Automatic start
startWhatsAppSession(currentAuthFolder).catch((err) => {
    emitToAndroid('FATAL_ERROR', { error: err.message || String(err) });
});
