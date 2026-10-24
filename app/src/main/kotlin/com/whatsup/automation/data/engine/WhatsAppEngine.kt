package com.whatsup.automation.data.engine

import android.content.Context
import com.whatsup.automation.data.security.SessionKeystore
import com.whatsup.automation.domain.model.ConnectionState
import com.whatsup.automation.domain.model.IncomingMessage
import com.whatsup.automation.domain.model.StatusStory
import com.whatsup.automation.domain.model.StatusMediaType
import com.whatsup.automation.domain.model.VaultItem
import com.whatsup.automation.domain.model.VaultMediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import com.whatsup.automation.domain.model.ActivityLog
import com.whatsup.automation.domain.model.LogStatus
import com.whatsup.automation.domain.repository.LogRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * المحرك المحلي لإدارة جلسة واتساب والتواصل مع طبقة البروتوكول.
 * يعمل بالكامل داخل الهاتف بدون خادم خارجي.
 */
@Singleton
class WhatsAppEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionKeystore: SessionKeystore,
    private val baileysBridgeManager: BaileysBridgeManager,
    private val logRepository: LogRepository,
    private val deviceContactsManager: com.whatsup.automation.data.local.contacts.DeviceContactsManager,
    private val outboxDao: com.whatsup.automation.data.local.dao.OutboxDao,
    private val vaultRepository: com.whatsup.automation.domain.repository.VaultRepository,
    private val statusDao: com.whatsup.automation.data.local.dao.StatusDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _connectionState = MutableStateFlow<ConnectionState>(
        if (sessionKeystore.isSessionActive()) ConnectionState.Connected else ConnectionState.Disconnected
    )
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _qrCode = MutableStateFlow<String?>(null)
    val qrCode: StateFlow<String?> = _qrCode.asStateFlow()

    private val _pairingCode = MutableStateFlow<String?>(null)
    val pairingCode: StateFlow<String?> = _pairingCode.asStateFlow()

    private val _pairingError = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val pairingError: SharedFlow<String> = _pairingError.asSharedFlow()

    private val _incomingMessages = MutableSharedFlow<IncomingMessage>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<IncomingMessage> = _incomingMessages.asSharedFlow()

    private val _incomingStatuses = MutableSharedFlow<StatusStory>(extraBufferCapacity = 64)
    val incomingStatuses: SharedFlow<StatusStory> = _incomingStatuses.asSharedFlow()

    private val enginePrefs = context.getSharedPreferences("whatsup_engine_prefs", Context.MODE_PRIVATE)
    private val _isAutomationPaused = MutableStateFlow(enginePrefs.getBoolean("automation_paused", false))
    val isAutomationPaused: StateFlow<Boolean> = _isAutomationPaused.asStateFlow()

    fun setAutomationPaused(paused: Boolean) {
        enginePrefs.edit().putBoolean("automation_paused", paused).apply()
        _isAutomationPaused.value = paused
        logDiagnosticEvent("ENGINE", if (paused) "⏸️ تم تجميد الأتمتة مؤقتاً (كتم الردود والحفظ مع بقاء الاتصال آمناً)." else "▶️ تم استئناف الأتمتة والردود بنجاح.")
    }

    fun toggleAutomationPause() {
        setAutomationPaused(!_isAutomationPaused.value)
    }

    private val _engineDiagnostics = MutableStateFlow<List<String>>(
        listOf("[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}][SYSTEM] Engine initialized successfully.")
    )
    val engineDiagnostics: StateFlow<List<String>> = _engineDiagnostics.asStateFlow()

    fun logDiagnosticEvent(tag: String, message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        val entry = "[$timestamp][$tag] $message"
        _engineDiagnostics.update { current ->
            (current + entry).takeLast(100)
        }
    }

    init {
        logDiagnosticEvent("ENGINE", "Engine starting, checking session status...")
        // ربط مسجل جهات الاتصال لعرض أحداث أذونات وحفظ جهات الاتصال
        deviceContactsManager.diagnosticLogger = { msg ->
            logDiagnosticEvent("Contacts", msg)
        }
        // تنظيف دوري لأي جهات اتصال تالفة تم حفظها كمعرفات LID مشفرة سابقاً
        scope.launch(Dispatchers.IO) {
            delay(1500L)
            val cleaned = deviceContactsManager.cleanupLidContactsFromDevice()
            if (cleaned > 0) {
                logDiagnosticEvent("Contacts", "تم تنظيف $cleaned جهة اتصال LID تالفة من دليل الهاتف بنجاح.")
            }
        }
        // تسجيل الاستماع المباشر لكافة أحداث Baileys عبر الجسر
        baileysBridgeManager.setEventListener { rawLine ->
            scope.launch {
                handleRawEngineEvent(rawLine)
            }
        }
        // تفعيل طابور الإرسال التلقائي في الخلفية (Outbox Drainer)
        startOutboxDrainer()
    }

    private val handledMessageTimestamps = java.util.concurrent.ConcurrentHashMap<String, Long>()

    /**
     * تسجيل الرسالة كمعالجة لمنع تكرار الردود بين الخدمات المختلفة (مثل NotificationListener و Baileys).
     * يعتمد على مطابقة آخر 8 أرقام للهاتف لضمان تطابق الأرقام المحلية والدولية والأسماء.
     */
    fun markMessageHandled(senderPhone: String, text: String) {
        val cleanDigits = senderPhone.replace("[^0-9]".toRegex(), "")
        val phoneKey = if (cleanDigits.length >= 7) cleanDigits.takeLast(8) else senderPhone.trim()
        val key = "$phoneKey|${text.trim()}"
        val now = System.currentTimeMillis()
        handledMessageTimestamps[key] = now

        // إذا كان هناك اسم مرسل أو صيغة مختلفة، نسجل المفتاح الخام أيضاً
        val rawTrim = senderPhone.trim()
        if (rawTrim.isNotBlank() && rawTrim != phoneKey) {
            handledMessageTimestamps["$rawTrim|${text.trim()}"] = now
        }

        if (handledMessageTimestamps.size > 200) {
            val expireTime = now - 120_000L
            handledMessageTimestamps.entries.removeIf { it.value < expireTime }
        }
    }

    /**
     * محاولة حجز معالجة الرسالة ذرياً (Atomic Single-Flight Claim).
     * يعود بـ true إذا تم حجز الرسالة بنجاح ولم يسبق معالجتها،
     * ويعود بـ false إذا كانت الرسالة قد حُجزت أو عولجت مسبقاً من مسار آخر لمنع أي رد مزدوج.
     */
    fun tryClaimMessageProcessing(senderPhone: String, text: String, windowMillis: Long = 60_000L): Boolean {
        val cleanDigits = senderPhone.replace("[^0-9]".toRegex(), "")
        val phoneKey = if (cleanDigits.length >= 7) cleanDigits.takeLast(8) else senderPhone.trim()
        val key = "$phoneKey|${text.trim()}"
        val now = System.currentTimeMillis()

        synchronized(handledMessageTimestamps) {
            val timestamp = handledMessageTimestamps[key]
            if (timestamp != null && (now - timestamp) < windowMillis) {
                return false
            }

            val rawTrim = senderPhone.trim()
            if (rawTrim.isNotBlank()) {
                val rawTimestamp = handledMessageTimestamps["$rawTrim|${text.trim()}"]
                if (rawTimestamp != null && (now - rawTimestamp) < windowMillis) {
                    return false
                }
            }

            handledMessageTimestamps[key] = now
            if (rawTrim.isNotBlank() && rawTrim != phoneKey) {
                handledMessageTimestamps["$rawTrim|${text.trim()}"] = now
            }

            if (handledMessageTimestamps.size > 300) {
                val expireTime = now - 120_000L
                handledMessageTimestamps.entries.removeIf { it.value < expireTime }
            }

            return true
        }
    }


    private val _isDirectAutomationActive = MutableStateFlow(false)
    val isDirectAutomationActive: StateFlow<Boolean> = _isDirectAutomationActive.asStateFlow()

    /**
     * هل المحرك الأساسي (Baileys عبر الربط بالرقم) متصل وجاهز لمعالجة وإرسال كل شيء؟
     */
    fun isMasterEngineConnected(): Boolean = _connectionState.value is ConnectionState.Connected

    /**
     * فحص ما إذا كانت هناك جلسة واتساب نشطة ومحفوظة مسبقاً.
     */
    fun hasSavedSession(): Boolean {
        val authDir = java.io.File(context.filesDir, "baileys_auth")
        val credsFile = java.io.File(authDir, "creds.json")
        return sessionKeystore.isSessionActive() || (credsFile.exists() && credsFile.length() > 50)
    }

    /**
     * التحقق من الحالة الحقيقية للاتصال وتحديثها لجلسة Baileys.
     */
    fun refreshRealConnectionState(): Boolean {
        val isSessionActive = hasSavedSession()
        _connectionState.value = if (isSessionActive && baileysBridgeManager.isBridgeReady.value) {
            ConnectionState.Connected
        } else if (isSessionActive) {
            ConnectionState.Reconnecting()
        } else {
            ConnectionState.Disconnected
        }
        return isSessionActive
    }

    /**
     * بدء تشغيل المحرك: إذا كانت الجلسة موجودة مسبقاً يبدأ الجسر فورياً لإبقاء الأتمتة نشطة 24/7.
     * أما إذا لم تكن الجلسة مقترنة، ينتظر طلب الاقتران عند الطلب (On-Demand) لتوفير البطارية وتفادي انقطاعات المقبس المتكررة.
     */
    fun startEngine() {
        if (hasSavedSession()) {
            logDiagnosticEvent("ENGINE", "جلسة محفوظة موجودة، بدء تشغيل محرك Baileys تلقائياً...")
            baileysBridgeManager.start()
            _connectionState.value = if (baileysBridgeManager.isBridgeReady.value) ConnectionState.Connected else ConnectionState.Reconnecting()
        } else {
            logDiagnosticEvent("ENGINE", "لا توجد جلسة محفوظة — المحرك في وضع الانتظار عند الطلب لتوفير موارد الهاتف.")
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    /**
     * إيقاف المحرك وفصل الاتصال.
     */
    fun stopEngine() {
        baileysBridgeManager.stop()
        _connectionState.value = ConnectionState.Disconnected
        _qrCode.value = null
        _pairingCode.value = null
    }

    /**
     * توليد رمز QR ديناميكي للاقتران عبر مقبس Baileys.
     */
    fun generateNewQr() {
        scope.launch {
            _connectionState.value = ConnectionState.AwaitingPairing
            baileysBridgeManager.start()
        }
    }

    /**
     * طلب كود اقتران (Pairing Code) رسمي من خوادم واتساب عبر محرك Baileys.
     */
    fun requestPairingCode(phoneNumber: String) {
        scope.launch {
            _connectionState.value = ConnectionState.AwaitingPairing
            _pairingCode.value = null
            baileysBridgeManager.start()
            val cleanPhone = phoneNumber.filter { it.isDigit() }
            if (cleanPhone.isEmpty()) {
                _connectionState.value = ConnectionState.Error("رقم الهاتف غير صالح")
                return@launch
            }
            sessionKeystore.setSessionPhone(cleanPhone)

            val isReady = try {
                withTimeoutOrNull(12_000L) {
                    baileysBridgeManager.isBridgeReady.first { it }
                } ?: false
            } catch (e: Exception) {
                false
            }

            if (!isReady) {
                val err = "تعذر الاتصال بمحرك واتساب المحلي، يرجى إعادة المحاولة بعد ثوانٍ"
                _connectionState.value = ConnectionState.Error(err)
                _pairingError.emit(err)
                return@launch
            }

            delay(500)

            val success = baileysBridgeManager.requestPairingCode(cleanPhone)
            if (!success) {
                val err = "تعذر إرسال طلب كود الاقتران إلى المحرك المحلي"
                _connectionState.value = ConnectionState.Error(err)
                _pairingError.emit(err)
            }
        }
    }

    /**
     * تأكيد الاقتران بنجاح وتفعيل الجلسة.
     */
    fun confirmPairingSuccess(phone: String) {
        val authDir = java.io.File(context.filesDir, "baileys_auth")
        val credsFile = java.io.File(authDir, "creds.json")
        val isVerifiedOnDisk = credsFile.exists() && credsFile.length() > 50

        if (isVerifiedOnDisk) {
            sessionKeystore.setSessionActive(true)
        }
        if (phone.isNotEmpty()) {
            sessionKeystore.setSessionPhone(phone)
        }
        sessionKeystore.setLastConnectedTimestamp(System.currentTimeMillis())
        _connectionState.value = ConnectionState.Connected
        _qrCode.value = null
        _pairingCode.value = null
    }

    /**
     * تفعيل الأتمتة المباشرة للإشعارات في النظام.
     */
    fun activateDirectAutomation() {
        _isDirectAutomationActive.value = true
    }

    /**
     * تسجيل الخروج ومسح الجلسة المخزنة في Keystore وإعلام المحرك.
     */
    fun logout() {
        scope.launch {
            baileysBridgeManager.logout()
            handleSessionLoggedOut()
        }
    }

    /**
     * تنظيف شامل لكافة البيانات العالقة والمقابس القديمة لإعادة التهيئة من الصفر.
     */
    fun cleanStalePairingData() {
        logDiagnosticEvent("CLEANUP", "User initiated full stale data and socket cleanup.")
        baileysBridgeManager.cleanStaleData()
        try {
            val authDir = java.io.File(context.filesDir, "baileys_auth")
            if (authDir.exists()) {
                authDir.deleteRecursively()
            }
        } catch (_: Exception) {}
        sessionKeystore.clearSession()
        _connectionState.value = ConnectionState.Disconnected
        _qrCode.value = null
        _pairingCode.value = null
    }

    /**
     * تنظيف ومعالجة حالة الخروج محلياً دون إعادة إرسال أمر LOGOUT للمحرك لتفادي الحلقات التكرارية.
     */
    private fun handleSessionLoggedOut() {
        try {
            val authDir = java.io.File(context.filesDir, "baileys_auth")
            if (authDir.exists()) {
                authDir.deleteRecursively()
            }
        } catch (_: Exception) {}
        baileysBridgeManager.clearAuthState()
        sessionKeystore.clearSession()
        _connectionState.value = ConnectionState.Disconnected
        _qrCode.value = null
        _pairingCode.value = null
    }

    /**
     * إرسال رسالة نصية عبر مقبس واتساب الحقيقي مع دعم محاكي السلوك البشري.
     */
    suspend fun sendMessage(
        recipientPhone: String,
        messageText: String,
        typingDelayMs: Long? = null,
        markRead: Boolean = false,
        messageId: String? = null,
        readDelayMs: Long? = null
    ): Boolean {
        val cleanPhone = recipientPhone.filter { it.isDigit() }
        if (cleanPhone.length < 7) {
            logDiagnosticEvent("SEND", "Invalid phone number: $recipientPhone")
            return false
        }

        // إذا كان مقبس Baileys غير جاهز أو غير متصل، حفظ الرسالة مباشرة في طابور Outbox أوفلاين
        if (!baileysBridgeManager.isBridgeReady.value) {
            enqueueOutboxMessage(cleanPhone, messageText)
            logDiagnosticEvent("OUTBOX", "المقبس غير متصل — تم حفظ الرسالة في طابور الإرسال أوفلاين إلى $cleanPhone")
            return true
        }

        logDiagnosticEvent("SEND", "Sending message to $cleanPhone (${messageText.take(20)}...) [TypingDelay: ${typingDelayMs ?: "default"}ms, MarkRead: $markRead]")
        val sent = baileysBridgeManager.sendMessage(
            recipient = cleanPhone,
            text = messageText,
            typingDelayMs = typingDelayMs,
            markRead = markRead,
            messageId = messageId,
            readDelayMs = readDelayMs
        )
        if (!sent) {
            enqueueOutboxMessage(cleanPhone, messageText)
            logDiagnosticEvent("OUTBOX", "فشل الإرسال المباشر — تم وضع الرسالة في طابور الإرسال لإعادة المحاولة.")
        }
        return sent
    }

    /**
     * إدراج رسالة في طابور الإرسال المجدول أوفلاين.
     */
    suspend fun enqueueOutboxMessage(recipient: String, messageText: String, mediaUri: String? = null): Long {
        return outboxDao.insertMessage(
            com.whatsup.automation.data.local.entity.OutboxMessageEntity(
                recipient = recipient,
                messageText = messageText,
                mediaUri = mediaUri,
                status = "PENDING",
                nextRetryTimestamp = System.currentTimeMillis()
            )
        )
    }

    /**
     * تشغيل خيط معالجة طابور الإرسال أوفلاين دورياً عند اتصال المقبس.
     */
    private fun startOutboxDrainer() {
        scope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    delay(15_000L)
                    if (baileysBridgeManager.isBridgeReady.value && _connectionState.value is ConnectionState.Connected) {
                        val pending = outboxDao.getPendingMessages(System.currentTimeMillis(), limit = 3)
                        for (msg in pending) {
                            logDiagnosticEvent("OUTBOX", "محاولة إرسال رسالة من الطابور إلى ${msg.recipient}...")
                            val success = baileysBridgeManager.sendMessage(
                                recipient = msg.recipient,
                                text = msg.messageText
                            )
                            if (success) {
                                outboxDao.markSent(msg.id)
                                logDiagnosticEvent("OUTBOX", "✅ تم إرسال الرسالة المعلقة رقم ${msg.id} بنجاح.")
                            } else {
                                val nextRetry = msg.retryCount + 1
                                if (nextRetry >= 5) {
                                    outboxDao.markFailed(msg.id, "تجاوزت الحد الأقصى للمحاولات (5)")
                                    logDiagnosticEvent("OUTBOX", "❌ فشلت الرسالة رقم ${msg.id} نهائياً.")
                                } else {
                                    val backoffMs = (5000L * (1 shl nextRetry))
                                    outboxDao.updateMessage(
                                        msg.copy(
                                            retryCount = nextRetry,
                                            nextRetryTimestamp = System.currentTimeMillis() + backoffMs,
                                            lastError = "فشل الإرسال المؤقت"
                                        )
                                    )
                                    logDiagnosticEvent("OUTBOX", "تأجيل الرسالة رقم ${msg.id} للمحاولة رقم $nextRetry بعد ${backoffMs / 1000} ثانية.")
                                }
                            }
                            delay(2500L)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * إرسال تأكيد قراءة رسالة محددة.
     */
    suspend fun markMessageRead(chatJid: String, messageId: String, participant: String? = null): Boolean {
        return baileysBridgeManager.markMessageRead(chatJid, messageId, participant)
    }

    /**
     * محاكاة أو استقبال رسالة واردة من واتساب ومعالجتها عبر خط الأنابيب (Pipeline).
     */
    suspend fun receiveMessage(message: IncomingMessage) {
        _incomingMessages.emit(message)
    }

    /**
     * محاكاة سريعة لاختبار القواعد عبر شاشة المحاكي.
     */
    suspend fun simulateIncomingMessage(senderPhone: String, text: String): IncomingMessage {
        val incoming = IncomingMessage(
            id = "SIM_${UUID.randomUUID().toString().take(8)}",
            senderPhone = senderPhone,
            text = text,
            timestamp = Instant.now(),
            isGroupMessage = false
        )
        _incomingMessages.emit(incoming)
        return incoming
    }

    /**
     * استقبال أو محاكاة حالة جديدة من واتساب ونشرها في النظام.
     */
    suspend fun receiveStatus(status: StatusStory) {
        _incomingStatuses.emit(status)
    }

    /**
     * إرسال تأكيد مشاهدة الحالة إلى واتساب (يدعم وضع التخفي Ghost Mode).
     */
    suspend fun markStatusViewed(statusId: String, senderPhone: String = "", participant: String? = null, sendReceipt: Boolean = false): Boolean {
        logDiagnosticEvent("STATUS", "Marking status $statusId viewed (sender: $senderPhone, participant: $participant, sendReceipt: $sendReceipt)")
        val cmd = org.json.JSONObject().apply {
            put("action", "VIEW_STATUS")
            put("statusId", statusId)
            put("senderPhone", senderPhone)
            put("ghostMode", !sendReceipt)
            put("sendReceipt", sendReceipt)
            if (!participant.isNullOrBlank()) {
                put("participant", participant)
            }
        }
        return baileysBridgeManager.sendCommand(cmd)
    }

    /**
     * إرسال تفاعل إيموجي على الحالة إلى واتساب (رياكشن رسمي على سيرفر الستوري دون إرسال أي رسائل للشات الخاص).
     */
    suspend fun reactToStatus(statusId: String, emoji: String = "💚", senderPhone: String = "", participant: String? = null, sendAsChatReply: Boolean = false): Boolean {
        logDiagnosticEvent("STATUS", "Reacting $emoji to status $statusId (sender: $senderPhone, participant: $participant, sendAsChatReply: false)")
        val cmd = org.json.JSONObject().apply {
            put("action", "REACT_STATUS")
            put("statusId", statusId)
            put("senderPhone", senderPhone)
            put("emoji", emoji)
            put("sendAsChatReply", false)
            put("ghostMode", false)
            if (!participant.isNullOrBlank()) {
                put("participant", participant)
            }
        }
        return baileysBridgeManager.sendCommand(cmd)
    }

    /**
     * طلب جلب كل الحالات الموجودة (القديمة والجديدة) من واتساب.
     * يُستدعى تلقائياً عند الاتصال، ويمكن استدعاؤه يدوياً من الواجهة أو الخدمة.
     */
    suspend fun fetchAllStatuses(): Boolean {
        return baileysBridgeManager.fetchStatuses()
    }

    /**
     * طلب تنزيل وسائط الحالة عند الطلب من محرك واتساب.
     */
    suspend fun downloadStatusMedia(statusId: String): Boolean {
        val cmd = org.json.JSONObject().apply {
            put("action", "DOWNLOAD_STATUS_MEDIA")
            put("statusId", statusId)
        }
        return baileysBridgeManager.sendCommand(cmd)
    }

    /**
     * معالجة أحداث IPC الواردة من محرك Baileys المحلي (index.js).
     * صيغة الحدث: [WHATSAPP_EVENT]{"event":"...", "data":{...}}
     */
    suspend fun handleRawEngineEvent(rawLine: String) {
        if (!rawLine.startsWith("[WHATSAPP_EVENT]")) return
        val jsonPayload = rawLine.removePrefix("[WHATSAPP_EVENT]").trim()
        try {
            val json = org.json.JSONObject(jsonPayload)
            val event = json.optString("event")
            val data = json.optJSONObject("data") ?: org.json.JSONObject()

            logDiagnosticEvent("EVENT", "Received: $event")
            when (event) {
                "BRIDGE_READY" -> {
                    val authDir = java.io.File(context.filesDir, "baileys_auth")
                    val credsFile = java.io.File(authDir, "creds.json")
                    if (credsFile.exists() && credsFile.length() > 50 && sessionKeystore.isSessionActive()) {
                        logDiagnosticEvent("BRIDGE", "Local auth creds detected. Awaiting socket verification...")
                    } else {
                        sessionKeystore.clearSession()
                        logDiagnosticEvent("BRIDGE", "Clean install or inactive session. Awaiting pairing...")
                    }
                }
                "LOGGED_OUT" -> {
                    val reason = data.optString("reason", "تم تسجيل الخروج من الأجهزة المرتبطة في تطبيق واتساب")
                    logDiagnosticEvent("LOGOUT", "Session terminated by WhatsApp: $reason")
                    handleSessionLoggedOut()
                    _connectionState.value = ConnectionState.Error(reason)
                    com.whatsup.automation.data.service.SystemNotificationHelper.sendSystemNotification(
                        context,
                        "تنبيه انقطاع الجلسة ⚠️",
                        reason,
                        isError = true
                    )
                }
                "QR_CODE" -> {
                    val qr = data.optString("qr")
                    if (qr.isNotEmpty()) {
                        _qrCode.value = qr
                        _connectionState.value = ConnectionState.AwaitingPairing
                        logDiagnosticEvent("PAIRING", "QR Code received (${qr.take(15)}...)")
                    }
                }
                "PAIRING_CODE" -> {
                    val code = data.optString("code")
                    if (code.isNotEmpty()) {
                        _pairingCode.value = code
                        _connectionState.value = ConnectionState.AwaitingPairing
                        logDiagnosticEvent("PAIRING", "Pairing Code generated: $code")
                    }
                }
                "PAIRING_ERROR" -> {
                    val error = data.optString("error", "فشل طلب كود الاقتران من واتساب")
                    logDiagnosticEvent("PAIRING", "Error: $error")
                    _pairingCode.value = null
                    _connectionState.value = ConnectionState.Error(error)
                    _pairingError.emit(error)
                }
                "PAIRING_FAILED" -> {
                    val reason = data.optString("reason", "فشلت عملية الاقتران مع واتساب")
                    logDiagnosticEvent("PAIRING", "Failed: $reason")
                    _pairingCode.value = null
                    _connectionState.value = ConnectionState.Error(reason)
                    _pairingError.emit(reason)
                }
                "SESSION_PERSISTED" -> {
                    val phone = data.optString("phone").filter { it.isDigit() }
                    val isVerified = data.optBoolean("verified", true)
                    if (isVerified) {
                        logDiagnosticEvent("SECURITY", "Cryptographic session verified on disk for $phone")
                        sessionKeystore.setSessionActive(true)
                        if (phone.isNotEmpty()) {
                            sessionKeystore.setSessionPhone("+$phone")
                        }
                    }
                }
                "CONNECTION_STATE" -> {
                    val status = data.optString("status")
                    logDiagnosticEvent("CONNECTION", "State changed to $status")
                    when (status) {
                        "CONNECTED" -> {
                            val userObj = data.optJSONObject("user")
                            val phone = userObj?.optString("id")
                                ?.replace("@s.whatsapp.net", "")
                                ?.substringBefore(":")
                                ?.filter { it.isDigit() } ?: ""
                            confirmPairingSuccess(phone)
                            logDiagnosticEvent("CONNECTION", "Connected successfully as $phone")
                        }
                        "DISCONNECTED" -> {
                            val reconnecting = data.optBoolean("reconnecting", false)
                            _connectionState.value = if (reconnecting) {
                                ConnectionState.Reconnecting()
                            } else {
                                ConnectionState.Disconnected
                            }
                        }
                    }
                }
                "INCOMING_MESSAGE" -> {
                    val id = data.optString("id", UUID.randomUUID().toString())
                    val senderPhone = data.optString("senderPhone").takeIf { it.isNotBlank() && it != "null" } ?: ""
                    val realPhone = data.optString("realPhone").takeIf { it.isNotBlank() && it != "null" }
                    val lid = data.optString("lid").takeIf { it.isNotBlank() && it != "null" }
                    val isLid = data.optBoolean("isLid", false)
                    val rawSenderName = data.optString("senderName", "").takeIf { it != "null" } ?: ""
                    val text = data.optString("text")
                    val isGroup = data.optBoolean("isGroup", false)
                    val isViewOnce = data.optBoolean("isViewOnce", false)
                    val hasMedia = data.optBoolean("hasMedia", false)
                    val mediaTypeStr = data.optString("mediaType", "TEXT")
                    val ts = data.optLong("timestamp", System.currentTimeMillis())

                    val effectivePhone = realPhone ?: senderPhone

                    if (effectivePhone.isNotEmpty() && text.isNotEmpty()) {
                        val cleanDigits = effectivePhone.replace("[^0-9]".toRegex(), "")

                        // البحث وفق الأولويات الثلاث: 1. الهاتف، 2. واتساب/pushName، 3. جيميل
                        val resolved = deviceContactsManager.resolveContactNameWithPriority(effectivePhone, rawSenderName)
                        val senderName = resolved?.name
                            ?: if (!isLid && cleanDigits.length in 7..15) {
                                "+$cleanDigits"
                            } else {
                                "جهة اتصال غير مسجلة"
                            }

                        val senderDisplay = if (resolved != null) "${resolved.name} ($effectivePhone)" else senderName
                        logDiagnosticEvent("MESSAGE", "Incoming from $senderDisplay: \"${text.take(30)}\"")
                        val message = IncomingMessage(
                            id = id,
                            senderPhone = effectivePhone,
                            text = text,
                            timestamp = Instant.ofEpochMilli(ts),
                            isGroupMessage = isGroup,
                            senderName = senderName,
                            isLid = isLid,
                            realPhone = realPhone,
                            lid = lid
                        )
                        _incomingMessages.emit(message)
                        scope.launch {
                            val shouldTrack = isViewOnce || sessionKeystore.isPhoneTrackedForVault(effectivePhone)
                            if (shouldTrack) {
                                val resolvedMediaType = if (hasMedia) VaultMediaType.fromString(mediaTypeStr) else VaultMediaType.TEXT
                                vaultRepository.saveVaultItem(
                                    VaultItem(
                                        id = id,
                                        senderPhone = effectivePhone,
                                        senderName = senderName,
                                        mediaType = resolvedMediaType,
                                        isViewOnce = isViewOnce,
                                        isDeleted = false,
                                        originalText = text,
                                        timestamp = ts
                                    )
                                )
                            }
                        }
                    }
                }
                "INCOMING_STATUS", "STATUS_RECEIVED" -> {
                    val id = data.optString("id", UUID.randomUUID().toString())
                    val senderPhone = data.optString("senderPhone").takeIf { it.isNotBlank() && it != "null" } ?: ""
                    val participant = data.optString("participant").takeIf { it.isNotBlank() && it != "null" }
                    val realPhone = data.optString("realPhone").takeIf { it.isNotBlank() && it != "null" }
                    val lid = data.optString("lid").takeIf { it.isNotBlank() && it != "null" }
                    val isLid = data.optBoolean("isLid", false)
                    val rawSenderName = data.optString("senderName", "").takeIf { it != "null" } ?: ""
                    val mediaTypeStr = data.optString("mediaType", "TEXT")
                    val textContent = data.optString("textContent").takeIf { it.isNotEmpty() && it != "null" }
                    val mediaUrl = data.optString("mediaUrl").takeIf { it.isNotBlank() && it != "null" }
                    val ts = data.optLong("timestamp", System.currentTimeMillis())

                    val effectivePhone = realPhone ?: senderPhone
                    val cleanDigits = effectivePhone.replace("[^0-9]".toRegex(), "")

                    // البحث وفق الأولويات الثلاث: 1. الهاتف، 2. واتساب/pushName، 3. جيميل
                    val resolved = deviceContactsManager.resolveContactNameWithPriority(effectivePhone, rawSenderName)
                    val senderName = resolved?.name
                        ?: if (!isLid && cleanDigits.length in 7..15) {
                            "+$cleanDigits"
                        } else {
                            "جهة اتصال غير مسجلة"
                        }

                    val mediaType = try {
                        StatusMediaType.valueOf(mediaTypeStr)
                    } catch (_: Exception) {
                        StatusMediaType.TEXT
                    }

                    logDiagnosticEvent("STATUS", "New status from $senderName ($mediaType)")
                    val story = StatusStory(
                        id = id,
                        senderPhone = effectivePhone,
                        senderName = senderName,
                        mediaType = mediaType,
                        textContent = textContent,
                        mediaUrl = mediaUrl,
                        timestamp = Instant.ofEpochMilli(ts),
                        isLid = isLid,
                        realPhone = realPhone,
                        lid = lid,
                        participant = participant
                    )
                    _incomingStatuses.emit(story)
                }
                "ERROR", "FATAL_ERROR" -> {
                    val errorMsg = data.optString("error", "حدث خطأ في محرك واتساب")
                    logDiagnosticEvent("ERROR", errorMsg)
                    _connectionState.value = ConnectionState.Error(errorMsg)
                }
                "STATUS_VIEWED" -> {
                    val statusId = data.optString("id")
                    val rawPhone = data.optString("senderPhone", "")
                    val realPhone = data.optString("realPhone").takeIf { it.isNotEmpty() }
                    val rawSenderName = data.optString("senderName", "")
                    val effectivePhone = realPhone ?: rawPhone
                    val cleanPhone = effectivePhone.replace("[^0-9]".toRegex(), "")
                    val resolved = deviceContactsManager.resolveContactNameWithPriority(effectivePhone, rawSenderName)
                    val displayName = resolved?.name ?: if (cleanPhone.length in 7..15) "+$cleanPhone" else "حالة واتساب"

                    logDiagnosticEvent("STATUS", "Status viewed for $displayName ($statusId)")
                    try {
                        logRepository.insertLog(
                            ActivityLog(
                                senderPhone = if (cleanPhone.isNotEmpty()) "+$cleanPhone" else "status@broadcast",
                                messageText = "مشاهدة حالة: $displayName",
                                actionExecuted = "مشاهدة حالة واتساب ($displayName)",
                                status = LogStatus.SUCCESS
                            )
                        )
                    } catch (_: Exception) {}
                }
                "STATUS_REACTED" -> {
                    val statusId = data.optString("id")
                    val emoji = data.optString("emoji", "💚")
                    val rawPhone = data.optString("senderPhone", "")
                    val realPhone = data.optString("realPhone").takeIf { it.isNotEmpty() }
                    val rawSenderName = data.optString("senderName", "")
                    val effectivePhone = realPhone ?: rawPhone
                    val cleanPhone = effectivePhone.replace("[^0-9]".toRegex(), "")
                    val resolved = deviceContactsManager.resolveContactNameWithPriority(effectivePhone, rawSenderName)
                    val displayName = resolved?.name ?: if (cleanPhone.length in 7..15) "+$cleanPhone" else "حالة واتساب"

                    logDiagnosticEvent("STATUS", "Status reacted with $emoji for $displayName ($statusId)")
                    try {
                        logRepository.insertLog(
                            ActivityLog(
                                senderPhone = if (cleanPhone.isNotEmpty()) "+$cleanPhone" else "status@broadcast",
                                messageText = "تفاعل $emoji على حالة: $displayName",
                                actionExecuted = "تفاعل ($emoji) مع حالة $displayName",
                                status = LogStatus.SUCCESS
                            )
                        )
                    } catch (_: Exception) {}
                }
                "STATUS_FETCH_START" -> {
                    val msg = data.optString("message", "جارٍ جلب الحالات...")
                    logDiagnosticEvent("STATUS_FETCH", msg)
                }
                "STATUS_FETCH_DONE" -> {
                    val count = data.optInt("count", 0)
                    val msg = data.optString("message", "اكتمل جلب الحالات")
                    logDiagnosticEvent("STATUS_FETCH", "✅ $msg (عدد الحالات المجلوبة: $count)")
                }
                "WHATSAPP_CONTACTS_SYNC" -> {
                    val contactsArray = data.optJSONArray("contacts")
                    if (contactsArray != null && contactsArray.length() > 0) {
                        val pairs = mutableListOf<Pair<String, String>>()
                        for (i in 0 until contactsArray.length()) {
                            val c = contactsArray.optJSONObject(i) ?: continue
                            val phone = c.optString("phone")
                            val name = c.optString("name")
                            if (phone.isNotEmpty() && name.isNotEmpty()) {
                                pairs.add(Pair(phone, name))
                            }
                        }
                        deviceContactsManager.updateWhatsAppContacts(pairs)
                        logDiagnosticEvent("CONTACTS", "تمت مزامنة ${pairs.size} جهة اتصال محفوظة من واتساب.")
                    }
                }
                "WHATSAPP_CONTACT_UPDATED" -> {
                    val phone = data.optString("phone")
                    val name = data.optString("name")
                    if (phone.isNotEmpty() && name.isNotEmpty()) {
                        deviceContactsManager.updateSingleWhatsAppContact(phone, name)
                        logDiagnosticEvent("CONTACTS", "تحديث اسم جهة اتصال واتساب: $name ($phone)")
                    }
                }
                "MESSAGE_REVOKED" -> {
                    val revokedId = data.optString("id")
                    val senderPhone = data.optString("senderPhone", "")
                    val senderName = data.optString("senderName", "")
                    val originalText = data.optString("originalText", "")
                    val ts = data.optLong("timestamp", System.currentTimeMillis())
                    scope.launch {
                        if (sessionKeystore.isPhoneTrackedForVault(senderPhone)) {
                            vaultRepository.markMessageAsDeleted(revokedId)
                            if (originalText.isNotBlank()) {
                                vaultRepository.saveVaultItem(
                                    VaultItem(
                                        id = revokedId,
                                        senderPhone = senderPhone,
                                        senderName = senderName,
                                        mediaType = VaultMediaType.TEXT,
                                        isViewOnce = false,
                                        isDeleted = true,
                                        originalText = originalText,
                                        timestamp = ts
                                    )
                                )
                            }
                            try {
                                statusDao.markAsDeleted(revokedId, ts)
                            } catch (_: Exception) {}
                            logDiagnosticEvent("ANTI_DELETE", "🗑️ تم اكتشاف حذف رسالة وحفظها في الخزينة: $originalText ($senderPhone)")
                            try {
                                logRepository.insertLog(
                                    ActivityLog(
                                        timestamp = Instant.ofEpochMilli(ts),
                                        senderPhone = senderPhone.ifBlank { "مجهول" },
                                        messageText = originalText.ifBlank { "[رسالة محذوفة لدى الجميع]" },
                                        matchedRule = "كاشف الحذف (Anti-Delete)",
                                        actionExecuted = "تم توثيق وحفظ الرسالة المحذوفة",
                                        status = LogStatus.SUCCESS,
                                        extractedName = senderName.ifBlank { null }
                                    )
                                )
                            } catch (_: Exception) {}
                        }
                    }
                }
                "MESSAGE_EDITED" -> {
                    val editId = data.optString("id")
                    val senderPhone = data.optString("senderPhone", "")
                    val senderName = data.optString("senderName", "")
                    val previousText = data.optString("previousText", "")
                    val newText = data.optString("newText", "")
                    val ts = data.optLong("timestamp", System.currentTimeMillis())
                    scope.launch {
                        if (sessionKeystore.isPhoneTrackedForVault(senderPhone)) {
                            val combinedText = "✏️ النص السابق: $previousText\n⬅️ النص المعدل: $newText"
                            vaultRepository.saveVaultItem(
                                VaultItem(
                                    id = editId,
                                    senderPhone = senderPhone,
                                    senderName = senderName,
                                    mediaType = VaultMediaType.TEXT,
                                    isViewOnce = false,
                                    isDeleted = false,
                                    originalText = combinedText,
                                    timestamp = ts
                                )
                            )
                            logDiagnosticEvent("ANTI_EDIT", "✏️ رصد تعديل رسالة من $senderPhone: \"$previousText\" ⬅️ \"$newText\"")
                            try {
                                logRepository.insertLog(
                                    ActivityLog(
                                        timestamp = Instant.ofEpochMilli(ts),
                                        senderPhone = senderPhone.ifBlank { "مجهول" },
                                        messageText = "الأصل: $previousText | الجديد: $newText",
                                        matchedRule = "كاشف تعديل الرسائل (Anti-Edit)",
                                        actionExecuted = "تم حفظ النص السابق قبل التعديل",
                                        status = LogStatus.SUCCESS,
                                        extractedName = senderName.ifBlank { null }
                                    )
                                )
                            } catch (_: Exception) {}
                        }
                    }
                }
                "STATUS_MEDIA_DOWNLOADED" -> {
                    val statusId = data.optString("id")
                    val mediaUrl = data.optString("mediaUrl").takeIf { it.isNotBlank() && it != "null" }
                    val success = data.optBoolean("success", false)
                    if (success && !mediaUrl.isNullOrBlank()) {
                        scope.launch {
                            try {
                                statusDao.updateMediaUrl(statusId, mediaUrl)
                                logDiagnosticEvent("STATUS", "✅ تم اكتمال تنزيل وسائط الحالة ($statusId): $mediaUrl")
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }
}
