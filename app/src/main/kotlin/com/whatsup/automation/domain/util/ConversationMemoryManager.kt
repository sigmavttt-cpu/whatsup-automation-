package com.whatsup.automation.domain.util

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * نموذج يمثل دورة حوارية واحدة (رسالة واردة ورد مرسل وسياقها).
 */
data class ConversationTurn(
    val incomingText: String,
    val replySent: String? = null,
    val intent: ConversationIntent = ConversationIntent.GENERAL_CHAT,
    val extractedName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ConversationIntent {
    CONTACT_SAVE_REQUEST,
    CONTACT_JUST_SAVED,
    CONTACT_CORRECTION,
    QUESTION_OR_INQUIRY,
    GENERAL_CHAT
}

/**
 * حالة المسار الحواري المتعدد الخطوات (Finite State Machine State).
 */
data class DialogueState(
    val stateName: String,
    val stepData: Map<String, String> = emptyMap(),
    val expiresAt: Long = System.currentTimeMillis() + 900_000L // 15 دقيقة افتراضياً
)

/**
 * مدير الذاكرة السياقية للمحادثات (Conversation Context Memory Engine).
 * يحتفظ بنافذة منزلقة لآخر 10 رسائل لكل محادثة لفهم التتابع الحواري
 * والتمييز بين: (طلب التسجيل، الاستدراك والتصحيح، السؤال، والدردشة العامة)،
 * ويدير حالات المحادثة المتعددة الخطوات (Finite State Machine).
 */
@Singleton
class ConversationMemoryManager @Inject constructor() {

    // ذاكرة حية منزلقة: رقم الهاتف -> قائمة بآخر 10 دورات حوارية
    private val memoryMap = ConcurrentHashMap<String, MutableList<ConversationTurn>>()

    // خريطة حالات الحوار النشطة (FSM): رقم الهاتف -> الحالة الحوارية الحالية
    private val stateMap = ConcurrentHashMap<String, DialogueState>()

    companion object {
        private const val MAX_TURNS_PER_USER = 10
        private const val RECENT_SAVE_WINDOW_MS = 180_000L // 3 دقائق بعد التسجيل الناجح
    }

    private fun getNormalizedKey(phone: String): String {
        val digits = phone.replace("[^0-9]".toRegex(), "")
        return if (digits.length >= 7) digits.takeLast(9) else phone.trim()
    }

    /**
     * تسجيل دورة حوارية جديدة في ذاكرة المحادثة.
     */
    fun recordTurn(
        phone: String,
        incomingText: String,
        replySent: String? = null,
        intent: ConversationIntent = ConversationIntent.GENERAL_CHAT,
        extractedName: String? = null
    ) {
        val key = getNormalizedKey(phone)
        val turn = ConversationTurn(
            incomingText = incomingText.trim(),
            replySent = replySent?.trim(),
            intent = intent,
            extractedName = extractedName?.trim(),
            timestamp = System.currentTimeMillis()
        )

        val list = memoryMap.getOrPut(key) { mutableListOf() }
        synchronized(list) {
            list.add(turn)
            if (list.size > MAX_TURNS_PER_USER) {
                list.removeAt(0)
            }
        }
    }

    /**
     * جلب آخر دورة حوارية للمرسل.
     */
    fun getLastTurn(phone: String): ConversationTurn? {
        val key = getNormalizedKey(phone)
        val list = memoryMap[key] ?: return null
        return synchronized(list) {
            list.lastOrNull()
        }
    }

    /**
     * هل تم حفظ هذا الشخص حديثاً (خلال آخر 3 دقائق)؟
     */
    fun isRecentlySaved(phone: String): Boolean {
        val lastTurn = getLastTurn(phone) ?: return false
        val now = System.currentTimeMillis()
        return (lastTurn.intent == ConversationIntent.CONTACT_JUST_SAVED || lastTurn.intent == ConversationIntent.CONTACT_SAVE_REQUEST) &&
                (now - lastTurn.timestamp) < RECENT_SAVE_WINDOW_MS
    }

    /**
     * فحص هل الرسالة الحالية تمثل استدراكاً أو تصحيحاً حقيقياً لاسم سابق.
     * أمثلة: "اقصد راكان", "قصدي راكان", "غلطت اقصد محمد", "عدل اسمي الى راكان", "سجلني قلت لك راكان".
     * يمنع منعاً باتاً اعتبار أي جملة تبدأ بـ "لا" أو "ياخي" تصحيحاً للاسم إلا باقترانها بلفظ الاستدراك الصريح.
     */
    fun isCorrectionIntent(text: String): Boolean {
        val trimmed = text.trim()
        val norm = ArabicMorphologyHelper.normalize(trimmed)
        if (norm.isBlank()) return false

        val explicitCorrectionPhrases = listOf(
            "اقصد", "قصدي", "غلطت", "عدل اسمي", "غير اسمي", "قلت لك اسمي", "قلتلك اسمي", "قلت لك"
        )
        return explicitCorrectionPhrases.any { norm.contains(it) }
    }

    /**
     * فحص هل الرسالة الحالية تمثل سؤالاً أو استفساراً أو سوالف عادية وليست طلباً لحفظ اسم.
     * أمثلة: "شروف واللي يدخلو...", "هل سجلتني؟", "ليش ما ترد؟", "كيفك؟".
     */
    fun isQuestionOrConversationalQuery(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.contains("؟") || trimmed.contains("?")) return true

        val norm = ArabicMorphologyHelper.normalize(trimmed)
        val tokens = norm.split(Regex("""[\s,،.:;!؟?/-]+""")).filter { it.isNotBlank() }

        // أدوات الاستفهام المستقلة (مطابقة دقيقة ككلمة مستقلة وليست كجزء من اسم مثل "عمار" أو "جمال")
        val standaloneQuestionParticles = setOf(
            "هل", "ليش", "لماذا", "كيف", "متى", "شو", "ايش", "ما", "مين", "فين", "وين"
        )
        if (tokens.any { standaloneQuestionParticles.contains(it) }) {
            // استثناء: إذا كان النص يبدأ بعبارة تسجيل اسم صريحة فلا يُعتبر سؤالاً
            val isExplicitNameIntro = norm.startsWith("اسمي") || norm.startsWith("انا اسمي") || 
                    norm.startsWith("سجلني") || norm.startsWith("احفظني") || norm.startsWith("سجل اسمي")
            if (!isExplicitNameIntro) {
                return true
            }
        }

        // عبارات السوالف والمحادثات العادية
        val conversationalPhrases = listOf(
            "من هو", "واللي", "ويسكتو", "مايقولو", "ما يقولو", "تشتيني", "صليت", "كنت", "ما اسمع", "باروح", "اكمل", "ممنوع"
        )
        return conversationalPhrases.any { norm.contains(ArabicMorphologyHelper.normalize(it)) }
    }

    /**
     * جلب آخر N دورات حوارية للمرسل (نافذة السياق المنزلقة).
     */
    fun getRecentTurns(phone: String, limit: Int = MAX_TURNS_PER_USER): List<ConversationTurn> {
        val key = getNormalizedKey(phone)
        val list = memoryMap[key] ?: return emptyList()
        return synchronized(list) {
            list.takeLast(limit)
        }
    }

    /**
     * استرجاع الذاكرة السياقية من سجلات Room عند بداية المحادثة أو بعد إعادة تشغيل التطبيق.
     */
    fun hydrateFromRecentLogs(phone: String, logs: List<com.whatsup.automation.domain.model.ActivityLog>) {
        val key = getNormalizedKey(phone)
        val list = memoryMap.getOrPut(key) { mutableListOf() }
        synchronized(list) {
            if (list.isNotEmpty()) return // الذاكرة محملة بالفعل

            // ترتيب السجلات زمنياً تصاعدياً لضمان التسلسل الصحيح للحوار
            val sortedLogs = logs.sortedBy { it.timestamp }
            for (log in sortedLogs) {
                list.add(
                    ConversationTurn(
                        incomingText = log.messageText,
                        replySent = if (log.actionExecuted.startsWith("إرسال رد")) log.actionExecuted else null,
                        intent = ConversationIntent.GENERAL_CHAT,
                        extractedName = log.extractedName,
                        timestamp = log.timestamp.toEpochMilli()
                    )
                )
            }
            while (list.size > MAX_TURNS_PER_USER) {
                list.removeAt(0)
            }
        }
    }

    /**
     * تعيين حالة الحوار الحالية للعميل (State Machine Transition).
     */
    fun setDialogueState(
        phone: String,
        stateName: String,
        stepData: Map<String, String> = emptyMap(),
        timeoutMs: Long = 900_000L
    ) {
        val key = getNormalizedKey(phone)
        stateMap[key] = DialogueState(
            stateName = stateName,
            stepData = stepData,
            expiresAt = System.currentTimeMillis() + timeoutMs
        )
    }

    /**
     * الحصول على حالة الحوار الحالية للعميل إن وجدت ولم تنتهِ صلاحيتها.
     */
    fun getDialogueState(phone: String): DialogueState? {
        val key = getNormalizedKey(phone)
        val state = stateMap[key] ?: return null
        if (System.currentTimeMillis() > state.expiresAt) {
            stateMap.remove(key)
            return null
        }
        return state
    }

    /**
     * مسح حالة الحوار والعودة للحالة الطبيعية.
     */
    fun clearDialogueState(phone: String) {
        val key = getNormalizedKey(phone)
        stateMap.remove(key)
    }

    /**
     * تنظيف الذاكرة للمحادثات القديمة وحالات الحوار منتهية الصلاحية (كل ساعة).
     */
    fun pruneStaleMemories(olderThanMs: Long = 3600_000L) {
        val now = System.currentTimeMillis()
        memoryMap.entries.removeIf { entry ->
            val last = entry.value.lastOrNull()
            last == null || (now - last.timestamp) > olderThanMs
        }
        stateMap.entries.removeIf { entry ->
            now > entry.value.expiresAt
        }
    }
}
