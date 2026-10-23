package com.whatsup.automation

import com.whatsup.automation.domain.model.ActivityLog
import com.whatsup.automation.domain.model.IncomingMessage
import com.whatsup.automation.domain.model.LogStatus
import com.whatsup.automation.domain.model.PatternType
import com.whatsup.automation.domain.model.Rule
import com.whatsup.automation.domain.model.RuleAction
import com.whatsup.automation.domain.nlp.ArabicFrustrationDetector
import com.whatsup.automation.domain.usecase.MatchRuleUseCase
import com.whatsup.automation.domain.util.ConversationMemoryManager
import com.whatsup.automation.domain.util.MessageBurstBuffer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * اختبارات المنظومة الشاملة لفهم النصوص وحل نقاط الضعف:
 * 1. مخزن تجميع الرسائل المتتالية السريعة (MessageBurstBuffer)
 * 2. كاشف الغضب والتصعيد والشكاوى الحادة (ArabicFrustrationDetector)
 * 3. مطابقة القواعد بالبحث التقريبي بشجرة ليفنشتاين (Levenshtein Trie Automaton)
 * 4. إدارة حالة الحوار المتعدد الخطوات واسترجاع الذاكرة (Dialogue State Machine & Hydration)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IntelligentTextUnderstandingArchitectureTest {

    // ═══════════════════════════════════════════════════════════
    // 1. اختبارات مخزن تجميع الرسائل المتتالية (MessageBurstBuffer)
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `test message burst buffer merges rapid consecutive messages into single cohesive text`() = runTest {
        val buffer = MessageBurstBuffer()
        val aggregatedList = mutableListOf<IncomingMessage>()

        val msg1 = IncomingMessage(
            id = "msg1",
            senderPhone = "967771234567",
            text = "سلام عليكم",
            timestamp = Instant.now()
        )
        val msg2 = IncomingMessage(
            id = "msg2",
            senderPhone = "967771234567",
            text = "بكم الحذاء الرياضي؟",
            timestamp = Instant.now()
        )
        val msg3 = IncomingMessage(
            id = "msg3",
            senderPhone = "967771234567",
            text = "مقاس 42",
            timestamp = Instant.now()
        )

        // إرسال 3 رسائل في أوقات متقاربة
        buffer.enqueue(msg1, this, debounceWindowMs = 2000L) { aggregatedList.add(it) }
        advanceTimeBy(500L)
        buffer.enqueue(msg2, this, debounceWindowMs = 2000L) { aggregatedList.add(it) }
        advanceTimeBy(500L)
        buffer.enqueue(msg3, this, debounceWindowMs = 2000L) { aggregatedList.add(it) }

        // قبل انتهاء الـ 2000ms يجب ألا يُطلق أي رد
        assertEquals(0, aggregatedList.size)

        // بعد مرور مهلة الـ 2000ms من آخر رسالة
        advanceTimeBy(2100L)
        advanceUntilIdle()
        assertEquals(1, aggregatedList.size)

        val finalAggregated = aggregatedList.first()
        val expectedText = "سلام عليكم\nبكم الحذاء الرياضي؟\nمقاس 42"
        assertEquals(expectedText, finalAggregated.text)
        assertEquals("msg3", finalAggregated.id)
    }

    @Test
    fun `test message burst buffer emits immediately for single message after window expires`() = runTest {
        val buffer = MessageBurstBuffer()
        val aggregatedList = mutableListOf<IncomingMessage>()

        val singleMsg = IncomingMessage(
            id = "single1",
            senderPhone = "967779998888",
            text = "كم السعر؟",
            timestamp = Instant.now()
        )

        buffer.enqueue(singleMsg, this, debounceWindowMs = 1500L) { aggregatedList.add(it) }
        advanceTimeBy(1000L)
        assertEquals(0, aggregatedList.size)

        advanceTimeBy(600L)
        advanceUntilIdle()
        assertEquals(1, aggregatedList.size)
        assertEquals("كم السعر؟", aggregatedList.first().text)
    }

    // ═══════════════════════════════════════════════════════════
    // 2. اختبارات كاشف الغضب والتصعيد (ArabicFrustrationDetector)
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `test arabic frustration detector detects severe complaints and anger`() {
        val angryMsg1 = "أقسم بالله خدمتكم فاشلة وزفت وين طلبي صار لي أسبوع؟؟؟"
        val result1 = ArabicFrustrationDetector.analyze(angryMsg1)
        assertTrue(result1.isHighFrustration)
        assertTrue(result1.frustrationScore >= 0.70f)
        assertTrue(result1.detectedReasons.isNotEmpty())

        val threatMsg = "يا نصابين راح اشتكي عليكم في الشرطة وحماية المستهلك!"
        val result2 = ArabicFrustrationDetector.analyze(threatMsg)
        assertTrue(result2.isHighFrustration)
        assertTrue(result2.frustrationScore >= 0.60f)

        val delayedMsg = "وين الشحنة ليش التأخير ما تردون ألووو؟"
        val result3 = ArabicFrustrationDetector.analyze(delayedMsg)
        assertTrue(result3.isHighFrustration)
    }

    @Test
    fun `test arabic frustration detector passes normal polite inquiries`() {
        val politeMsg1 = "السلام عليكم ورحمة الله، لو سمحت كم سعر المنتج مع التوصيل؟"
        val result1 = ArabicFrustrationDetector.analyze(politeMsg1)
        assertFalse(result1.isHighFrustration)
        assertEquals(0.0f, result1.frustrationScore)

        val politeMsg2 = "شكراً جزيلاً ما قصرت ربي يحفظك"
        val result2 = ArabicFrustrationDetector.analyze(politeMsg2)
        assertFalse(result2.isHighFrustration)
    }

    // ═══════════════════════════════════════════════════════════
    // 3. اختبارات مطابقة القواعد بشجرة ليفنشتاين التلقائية
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `test match rule use case matches typos using fuzzy match mode`() {
        val ruleRepoMock = object : com.whatsup.automation.domain.repository.RuleRepository {
            override fun getAllRules() = kotlinx.coroutines.flow.emptyFlow<List<Rule>>()
            override fun getEnabledRules() = kotlinx.coroutines.flow.emptyFlow<List<Rule>>()
            override suspend fun getRuleById(id: Long) = null
            override suspend fun insertRule(rule: Rule) = 1L
            override suspend fun updateRule(rule: Rule) {}
            override suspend fun deleteRule(id: Long) {}
            override suspend fun toggleRule(id: Long, enabled: Boolean) {}
        }

        val matcher = MatchRuleUseCase(ruleRepoMock)

        val fuzzyRule = Rule(
            id = 1,
            name = "سعر المنتج",
            description = "الرد بالأسعار",
            patternType = PatternType.FUZZY_MATCH,
            patternValue = "الأسعار",
            actions = listOf(RuleAction.SendReply("سعرنا 100 ريال")),
            priority = 1
        )

        // مطابقة كلمة قريبة مع خطأ إملائي بالياء أو الهمزة
        assertTrue("يجب أن تطابق 'الاسعار'", matcher.matches(fuzzyRule, "بكم الاسعار عندكم؟"))
        assertTrue("يجب أن تطابق 'اسعاركم'", matcher.matches(fuzzyRule, "كم اسعاركم اليوم؟"))
        assertTrue("يجب أن تطابق 'الاسعارر' مع خطأ حرف زائد", matcher.matches(fuzzyRule, "ابغى اعرف الاسعارر"))
    }

    @Test
    fun `test contains mode automatically tolerates 1 typo via Levenshtein Trie`() {
        val ruleRepoMock = object : com.whatsup.automation.domain.repository.RuleRepository {
            override fun getAllRules() = kotlinx.coroutines.flow.emptyFlow<List<Rule>>()
            override fun getEnabledRules() = kotlinx.coroutines.flow.emptyFlow<List<Rule>>()
            override suspend fun getRuleById(id: Long) = null
            override suspend fun insertRule(rule: Rule) = 1L
            override suspend fun updateRule(rule: Rule) {}
            override suspend fun deleteRule(id: Long) {}
            override suspend fun toggleRule(id: Long, enabled: Boolean) {}
        }

        val matcher = MatchRuleUseCase(ruleRepoMock)

        val containsRule = Rule(
            id = 2,
            name = "حجز موعد",
            description = "الحجوزات",
            patternType = PatternType.CONTAINS,
            patternValue = "حجوزات",
            actions = listOf(RuleAction.SendReply("أهلاً بك في قسم الحجوزات")),
            priority = 1
        )

        // مطابقة مطبعية طفيفة (مسافة تعديل 1)
        assertTrue("يجب أن يسامح مع خطأ حرف مثل 'حجوازت'", matcher.matches(containsRule, "اريد حجوازت"))
    }

    // ═══════════════════════════════════════════════════════════
    // 4. اختبارات إدارة حالة الحوار واسترجاع الذاكرة (FSM & Memory)
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `test dialogue state machine sets, retrieves and clears states`() {
        val memoryManager = ConversationMemoryManager()
        val phone = "+967771122334"

        assertNull(memoryManager.getDialogueState(phone))

        // ضبط حالة العميل في خطوة اختيار الفرع
        memoryManager.setDialogueState(
            phone = phone,
            stateName = "AWAITING_BRANCH",
            stepData = mapOf("orderId" to "ORD-101")
        )

        val state = memoryManager.getDialogueState(phone)
        assertNotNull(state)
        assertEquals("AWAITING_BRANCH", state?.stateName)
        assertEquals("ORD-101", state?.stepData?.get("orderId"))

        // مسح الحالة عند اكتمال الخطوة
        memoryManager.clearDialogueState(phone)
        assertNull(memoryManager.getDialogueState(phone))
    }

    @Test
    fun `test hydration from recent Room logs restores context window`() {
        val memoryManager = ConversationMemoryManager()
        val phone = "967773344556"

        assertEquals(0, memoryManager.getRecentTurns(phone).size)

        // محاكاة سجلات سابقة قادمة من قاعدة بيانات Room
        val pastLogs = listOf(
            ActivityLog(
                id = 10,
                timestamp = Instant.now().minusSeconds(120),
                senderPhone = phone,
                messageText = "هل متوفر التوصيل لصنعاء؟",
                matchedRule = "التوصيل",
                actionExecuted = "إرسال رد (نعم متوفر لجميع المحافظات)",
                status = LogStatus.SUCCESS
            ),
            ActivityLog(
                id = 11,
                timestamp = Instant.now().minusSeconds(60),
                senderPhone = phone,
                messageText = "وكم يستغرق الوقت؟",
                matchedRule = "مدة التوصيل",
                actionExecuted = "إرسال رد (خلال 24 ساعة)",
                status = LogStatus.SUCCESS
            )
        )

        memoryManager.hydrateFromRecentLogs(phone, pastLogs)

        val turns = memoryManager.getRecentTurns(phone)
        assertEquals(2, turns.size)
        assertEquals("هل متوفر التوصيل لصنعاء؟", turns[0].incomingText)
        assertEquals("وكم يستغرق الوقت؟", turns[1].incomingText)
        assertEquals("وكم يستغرق الوقت؟", memoryManager.getLastTurn(phone)?.incomingText)
    }
}
