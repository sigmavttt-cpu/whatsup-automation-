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
import { downloadAndDecryptMedia } from './media-downloader.js';
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
 * فك التغليف العودي الدقيق للرسائل المركبة (deviceSentMessage, ephemeralMessage, viewOnceMessage, إلخ)
 * وتحديد وسائط العرض لمرة واحدة بشكل موثوق 100%.
 */
function unwrapAndInspectMessage(rawMsg) {
    let current = rawMsg;
    let isViewOnce = false;
    let depth = 0;
    while (current && depth < 10) {
        depth++;
        if (current.viewOnceMessage?.message) {
            isViewOnce = true;
            current = current.viewOnceMessage.message;
            continue;
        }
        if (current.viewOnceMessageV2?.message) {
            isViewOnce = true;
            current = current.viewOnceMessageV2.message;
            continue;
        }
        if (current.viewOnceMessageV2Extension?.message) {
            isViewOnce = true;
            current = current.viewOnceMessageV2Extension.message;
            continue;
        }
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

    if (extracted?.imageMessage?.viewOnce ||
        extracted?.videoMessage?.viewOnce ||
        extracted?.audioMessage?.viewOnce ||
        extracted?.viewOnce ||
        rawMsg?.imageMessage?.viewOnce ||
        rawMsg?.videoMessage?.viewOnce ||
        rawMsg?.audioMessage?.viewOnce) {
        isViewOnce = true;
    }

    return { leaf: extracted, isViewOnce };
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

                // Cache raw message for PreKey retry decryption requests
                if (msg.key?.id && msg.message) {
                    rawMessagesCache.set(msg.key.id, msg.message);
                    if (rawMessagesCache.size > CONFIG.MAX_RAW_MESSAGES) {
                        const firstKey = rawMessagesCache.keys().next().value;
                        rawMessagesCache.delete(firstKey);
                    }
                }

                // Status stories
                if (remoteJid === 'status@broadcast') {
                    if (msg.key?.fromMe || !msg.message) continue;
                    await processStatusMessage(msg, authFolder, sock);
                    continue;
                }

                // ⚡ النبض التشخيصي الحتمي الفوري: تسجيل وصول الحزمة فوراً لأندرويد قبل أي شروط
                const hasMessagePayload = !!msg.message;
                const stubCode = msg.messageStubType ? ` [Stub: ${msg.messageStubType}]` : '';
                emitToAndroid('VAULT_PROGRESS', {
                    status: 'MSG_INGEST',
                    id: msg.key?.id || '',
                    senderPhone: cleanRawDigits || actualSender,
                    message: `📩 استلام حزمة من (${cleanRawDigits || actualSender}) [ID: ${msg.key?.id ? msg.key.id.slice(-6) : 'N/A'}] | محتوى: ${hasMessagePayload ? 'جاهز' : 'مشفر/Stub'}${stubCode}`
                });

                if (!msg.message) {
                    console.log(`[MSG_STUB] Received empty/stub message from ${actualSender}:`, msg.messageStubType, msg.messageStubParameters);
                    continue;
                }

                // 2. فك التغليف العودي واكتشاف وسائط العرض لمرة واحدة بدقة 100%
                const { leaf, isViewOnce } = unwrapAndInspectMessage(msg.message);

                // السماح برسائل العرض لمرة واحدة حتى لو كانت fromMe لاختبار المستخدم مع نفسه أو الطرف الآخر
                if (msg.key?.fromMe && !isViewOnce) continue;

                const contentType = typeof getContentType === 'function' ? getContentType(leaf) : null;

                // 4. استخراج كائن الوسائط مباشرة
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
                const mediaTypeLower = isImage ? 'image' : (isVideo ? 'video' : (isAudio ? 'audio' : (isDoc ? 'document' : 'image')));
                const ext = isImage ? 'jpg' : (isVideo ? 'mp4' : (isAudio ? 'ogg' : 'bin'));
                const mime = mediaObj?.mimetype || (isImage ? 'image/jpeg' : (isVideo ? 'video/mp4' : (isAudio ? 'audio/ogg' : 'application/octet-stream')));

                // 🎯 الأولوية الصفرية الفورية: التقاط وفك تشفير وسائط العرض لمرة واحدة دون أي انتظار لحل الـ LID
                if (hasMedia && mediaObj && isViewOnce) {
                const vaultDir = path.join(authFolder, '..', 'vault_media');
                try {
                    if (!fs.existsSync(vaultDir)) fs.mkdirSync(vaultDir, { recursive: true });
                } catch (eDir) {
                    console.error('[VAULT] Failed to create vault directory:', eDir);
                }
                const filePath = path.join(vaultDir, `viewonce_${msg.key.id}.${ext}`);

                emitToAndroid('VAULT_PROGRESS', {
                    status: 'DETECTED',
                    id: msg.key.id,
                    senderPhone: cleanRawDigits || actualSender,
                    mediaType: mediaType,
                    message: `🔒 تم رصد ${mediaType === 'IMAGE' ? 'صورة' : 'وسائط'} عرض لمرة واحدة من ${cleanRawDigits || actualSender}، جاري فك التشفير والتنزيل...`
                });

                (async () => {
                    let buffer = null;
                    try {
                        buffer = await downloadAndDecryptMedia(mediaObj, mediaTypeLower, sock, msg);
                    } catch (e1) {
                        console.error(`[VAULT] downloadAndDecryptMedia (${mediaTypeLower}) error:`, e1.message);
                    }

                    if (buffer && buffer.length > 0) {
                        try {
                            fs.writeFileSync(filePath, buffer);
                            console.log(`[VAULT] View-Once captured successfully: ${filePath} (${buffer.length} bytes, type=${mediaType})`);

                            rawMessagesCache.set(`media_file_${msg.key.id}`, {
                                localFilePath: filePath,
                                mimeType: mime,
                                fileSizeBytes: buffer.length,
                                mediaType: mediaType,
                                originalText: mediaObj?.caption || ''
                            });

                            const senderDisplay = msg.pushName || getContactName(cleanRawDigits) || cleanRawDigits || actualSender;

                            emitToAndroid('VAULT_MEDIA_CAPTURED', {
                                id: msg.key.id,
                                senderPhone: cleanRawDigits || actualSender,
                                senderName: senderDisplay,
                                mediaType: mediaType,
                                isViewOnce: true,
                                originalText: mediaObj?.caption || '',
                                localFilePath: filePath,
                                mimeType: mime,
                                fileSizeBytes: buffer.length,
                                timestamp: msg.messageTimestamp ? Number(msg.messageTimestamp) * 1000 : Date.now()
                            });

                            emitToAndroid('VAULT_PROGRESS', {
                                status: 'SUCCESS',
                                id: msg.key.id,
                                senderPhone: cleanRawDigits || actualSender,
                                mediaType: mediaType,
                                message: `✅ تم فك تشفير وحفظ ${mediaType === 'IMAGE' ? 'الصورة' : 'الوسائط'} بنجاح في الخزينة (${Math.round(buffer.length / 1024)} KB)`
                            });
                        } catch (writeErr) {
                            console.error('[VAULT] Failed to write vault media file:', writeErr);
                            emitToAndroid('VAULT_PROGRESS', {
                                status: 'ERROR',
                                id: msg.key.id,
                                senderPhone: cleanRawDigits || actualSender,
                                message: `فشل كتابة ملف الوسائط على القرص: ${writeErr.message}`
                            });
                        }
                    } else {
                        console.warn('[VAULT] Failed to download media buffer for:', msg.key.id);
                        emitToAndroid('VAULT_PROGRESS', {
                            status: 'ERROR',
                            id: msg.key.id,
                            senderPhone: cleanRawDigits || actualSender,
                            message: `تعذر جلب ملف الوسائط (انتهت صلاحية الرابط من واتساب أو تم رفضه).`
                        });
                    }
                })().catch(topErr => {
                    console.error('[VAULT] Unhandled error in media capture:', topErr);
                });
            }

            // ربط جهات الاتصال وحل أرقام LID بهدوء لباقي العمليات (الرد التلقائي وإشعارات الرسائل)
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

            // 2. التقاط إشارة حذف الرسائل لدى الجميع (Anti-Delete Revocation)
            const protocolMsg = msg.message?.protocolMessage;
            if (protocolMsg && protocolMsg.type === 0 && protocolMsg.key?.id) {
                const revokedId = protocolMsg.key.id;
                const cachedMedia = rawMessagesCache.get(`media_file_${revokedId}`);
                if (cachedMedia) {
                    emitToAndroid('VAULT_MEDIA_CAPTURED', {
                        id: revokedId,
                        senderPhone: cleanPhone || actualSender,
                        senderName: msg.pushName || getContactName(cleanPhone) || '',
                        mediaType: cachedMedia.mediaType,
                        isViewOnce: false,
                        originalText: cachedMedia.originalText,
                        localFilePath: cachedMedia.localFilePath,
                        mimeType: cachedMedia.mimeType,
                        fileSizeBytes: cachedMedia.fileSizeBytes,
                        timestamp: Date.now()
                    });
                }
                emitToAndroid('MESSAGE_REVOKED', {
                    id: revokedId,
                    senderPhone: cleanPhone || actualSender,
                    timestamp: Date.now()
                });
                continue;
            }

            // 3. استخراج النص أو وضع نص بديل للوسائط بدون تعليق
            let text = leaf?.conversation ||
                       leaf?.extendedTextMessage?.text ||
                       leaf?.imageMessage?.caption ||
                       leaf?.videoMessage?.caption ||
                       leaf?.documentMessage?.caption ||
                       mediaObj?.caption ||
                       '';

            if (!text && hasMedia) {
                if (isImage) text = isViewOnce ? '📷 [صورة عرض لمرة واحدة]' : '📷 [صورة]';
                else if (isVideo) text = isViewOnce ? '🎥 [فيديو عرض لمرة واحدة]' : '🎥 [فيديو]';
                else if (isAudio) text = isViewOnce ? '🎙️ [تسجيل صوتي عرض لمرة واحدة]' : '🎙️ [رسالة صوتية]';
                else if (isDoc) text = '📄 [مستند]';
            }

            if (!text || !cleanPhone) continue;

            const senderName = msg.pushName || getContactName(cleanPhone) || '';

            emitToAndroid('INCOMING_MESSAGE', {
                id: msg.key.id,
                senderPhone: cleanPhone,
                realPhone: resolved.realPhone,
                lid: resolved.lid,
                isLid: resolved.isLid,
                senderName,
                text,
                isGroup,
                isViewOnce,
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
