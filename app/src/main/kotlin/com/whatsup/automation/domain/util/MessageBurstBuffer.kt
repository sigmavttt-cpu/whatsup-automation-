package com.whatsup.automation.domain.util

import com.whatsup.automation.domain.model.IncomingMessage
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مخزن تجميع الرسائل المتتالية السريعة (Message Burst Debounce Buffer).
 * يمنع إرسال ردود آلية متعددة متضاربة عندما يرسل العميل أفكاره مجزأة
 * في عدة رسائل سريعة خلال نافذة زمنية قصيرة (مثلاً ثانيتان).
 * يقوم بتجميع أسطر الرسائل في رسالة واحدة متماسكة قبل تمريرها لمحرك القواعد.
 */
@Singleton
class MessageBurstBuffer @Inject constructor() {

    private data class BurstSession(
        val messages: MutableList<IncomingMessage> = mutableListOf(),
        var job: Job? = null,
        val startTime: Long = System.currentTimeMillis()
    )

    private val sessions = ConcurrentHashMap<String, BurstSession>()

    companion object {
        const val DEFAULT_DEBOUNCE_WINDOW_MS = 2000L
        const val MAX_BURST_HOLD_MS = 5000L
    }

    private fun getNormalizedKey(phone: String): String {
        val digits = phone.replace("[^0-9]".toRegex(), "")
        return if (digits.length >= 7) digits.takeLast(9) else phone.trim()
    }

    /**
     * إدراج رسالة واردة في مخزن التجميع.
     *
     * @param message الرسالة الواردة من واتساب
     * @param scope نطاق الكوروتينز لجدولة مؤقت التجميع
     * @param debounceWindowMs مدة الانتظار بعد آخر رسالة (افتراضياً 2000 مللي ثانية)
     * @param onBurstReady دالة الاستدعاء عند اكتمال تجميع الرسائل
     */
    fun enqueue(
        message: IncomingMessage,
        scope: CoroutineScope,
        debounceWindowMs: Long = DEFAULT_DEBOUNCE_WINDOW_MS,
        onBurstReady: suspend (IncomingMessage) -> Unit
    ) {
        val key = getNormalizedKey(message.realPhone ?: message.senderPhone)

        // تجاهل التجميع للرسائل الوهمية أو مجموعات الأخبار
        if (key.isBlank() || message.isGroupMessage) {
            scope.launch { onBurstReady(message) }
            return
        }

        synchronized(sessions) {
            val session = sessions.getOrPut(key) { BurstSession() }
            session.messages.add(message)
            session.job?.cancel()

            val elapsed = System.currentTimeMillis() - session.startTime
            val effectiveDelay = if (elapsed >= MAX_BURST_HOLD_MS) {
                0L // تجاوز الحد الأقصى للحجز — إطلاق فوري
            } else {
                debounceWindowMs.coerceAtMost(MAX_BURST_HOLD_MS - elapsed)
            }

            session.job = scope.launch {
                if (effectiveDelay > 0) {
                    delay(effectiveDelay)
                }

                val messagesToProcess = synchronized(sessions) {
                    sessions.remove(key)?.messages?.toList() ?: emptyList()
                }

                if (messagesToProcess.isNotEmpty()) {
                    val aggregatedMessage = aggregateMessages(messagesToProcess)
                    onBurstReady(aggregatedMessage)
                }
            }
        }
    }

    /**
     * دمج مصفوفة رسائل في رسالة واحدة متماسكة.
     */
    fun aggregateMessages(messages: List<IncomingMessage>): IncomingMessage {
        if (messages.size == 1) return messages.first()

        val latest = messages.last()
        val mergedText = messages
            .map { it.text.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")

        return latest.copy(
            text = mergedText
        )
    }

    /**
     * تفريغ وإلغاء جميع الجلسات النشطة فوراً (عند إيقاف الخدمة).
     */
    fun clear() {
        synchronized(sessions) {
            sessions.values.forEach { it.job?.cancel() }
            sessions.clear()
        }
    }
}
