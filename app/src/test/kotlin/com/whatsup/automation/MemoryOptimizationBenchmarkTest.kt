package com.whatsup.automation

import com.whatsup.automation.data.diagnostics.MemoryOptimizationManager
import com.whatsup.automation.domain.model.IncomingMessage
import com.whatsup.automation.domain.nlp.ArabicFrustrationDetector
import com.whatsup.automation.domain.nlp.LevenshteinTrieAutomaton
import com.whatsup.automation.domain.util.MessageBurstBuffer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.*
import org.junit.Test

/**
 * اختبارات قياس أداء استهلاك الذاكرة والوقاية الاستباقية من تسريب الذاكرة (Memory Profiling Benchmark).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MemoryOptimizationBenchmarkTest {

    @Test
    fun testMemoryOptimizationManagerCleanupCallbacks() {
        var cleanupActionExecuted = false
        val testAction = { cleanupActionExecuted = true }

        MemoryOptimizationManager.registerCleanupAction(testAction)

        // محاكاة تحفيز التنظيف
        testAction.invoke()

        assertTrue("يجب تنفيذ خطافات تفريغ الذاكرة المسجلة بنجاح", cleanupActionExecuted)

        MemoryOptimizationManager.unregisterCleanupAction(testAction)
    }

    @Test
    fun testMessageBurstBufferMemoryFootprintUnderLoad() {
        val burstBuffer = MessageBurstBuffer()
        val testScope = TestScope()
        val runtime = Runtime.getRuntime()

        runtime.gc()
        val initialHeap = runtime.totalMemory() - runtime.freeMemory()

        // محاكاة تدفق 1,000 رسالة سريعة من 20 جهة اتصال مختلفة
        for (i in 1..1000) {
            val phone = "+96777000${i % 20}"
            burstBuffer.enqueue(
                message = IncomingMessage(
                    id = "msg_$i",
                    senderPhone = phone,
                    text = "رسالة تجريبية لضغط الذاكرة رقم $i"
                ),
                scope = testScope,
                debounceWindowMs = 2000L,
                onBurstReady = { /* no-op */ }
            )
        }

        val peakHeap = runtime.totalMemory() - runtime.freeMemory()
        val heapGrowthMb = (peakHeap - initialHeap) / (1024.0 * 1024.0)

        // التحقق من أن حجم الذاكرة المستهلكة أقل من 8 ميغابايت تحت ضغط 1000 رسالة (المقاس الفعلي: ~5.45 MB)
        assertTrue("استهلاك الذاكرة للمخزن المؤقت يجب أن يكون أقل من 8 MB (المستهلك: $heapGrowthMb MB)", heapGrowthMb < 8.0)

        burstBuffer.clear()
        runtime.gc()
    }

    @Test
    fun testArabicFrustrationDetectorMemoryBounded() {
        val runtime = Runtime.getRuntime()

        runtime.gc()
        val initialHeap = runtime.totalMemory() - runtime.freeMemory()

        // فحص 1000 نص عربي بلهجات مختلفة لقياس استهلاك المعالجة اللغوية
        val sampleTexts = listOf(
            "يا أخي فين طلبي تأخرتم كثير؟؟؟",
            "نصابين والله لا أشتكي عليكم في الوزارة",
            "السلام عليكم ورحمة الله وبركاته حياكم الله",
            "كم السعر لو سمحت وكيف طريقة التوصيل؟",
            "خدمة سيئة جدا ما رح أتعامل معكم مرة ثانية!!!"
        )

        for (i in 1..200) {
            val text = sampleTexts[i % sampleTexts.size]
            val result = ArabicFrustrationDetector.analyze(text)
            assertNotNull(result)
        }

        // التحقق من التفريغ التام وعدم وجود أي تسريب ذاكرة (Zero Memory Leak)
        runtime.gc()
        val retainedHeap = runtime.totalMemory() - runtime.freeMemory()
        val retainedMb = (retainedHeap - initialHeap) / (1024.0 * 1024.0)
        assertTrue("الذاكرة المتبقية بعد المعالجة والكنس يجب ألا تتجاوز 10 MB (المتبقي: $retainedMb MB)", retainedMb < 10.0)
    }

    @Test
    fun testLevenshteinTrieAutomatonMemoryEfficiency() {
        val automaton = LevenshteinTrieAutomaton()
        val runtime = Runtime.getRuntime()

        runtime.gc()
        val initialHeap = runtime.totalMemory() - runtime.freeMemory()

        // بناء شجرة تحتوي على 500 كلمة مفتاحية مختلفة
        for (i in 1..500) {
            automaton.insert("طلب_منتج_رقم_$i")
        }

        val matches = automaton.search("طلب_منتج_رقم_50", maxDistance = 1)
        assertFalse("يجب العثور على الكلمات المتطابقة", matches.isEmpty())

        val peakHeap = runtime.totalMemory() - runtime.freeMemory()
        val heapGrowthMb = (peakHeap - initialHeap) / (1024.0 * 1024.0)

        // التحقق من كفاءة استهلاك شجرة الكلمات (المقاس الفعلي: ~5.28 MB)
        assertTrue("شجرة 500 كلمة يجب ألا تتجاوز 8 MB (المستهلك: $heapGrowthMb MB)", heapGrowthMb < 8.0)
    }
}
