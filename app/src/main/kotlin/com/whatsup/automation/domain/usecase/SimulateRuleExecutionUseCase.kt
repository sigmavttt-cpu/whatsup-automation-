package com.whatsup.automation.domain.usecase

import com.whatsup.automation.domain.model.*
import com.whatsup.automation.domain.repository.RuleRepository
import com.whatsup.automation.domain.util.NameExtractorHelper
import com.whatsup.automation.domain.util.SpintaxEngine
import com.whatsup.automation.domain.util.StrictNegationAnalyzer
import com.whatsup.automation.domain.util.QuotedSpeechDetector
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class SimulationResult(
    val isMatched: Boolean,
    val matchedRuleName: String? = null,
    val matchedRulePriority: Int? = null,
    val patternType: String? = null,
    val patternValue: String? = null,
    val simulatedReply: String? = null,
    val extractedName: String? = null,
    val isNegated: Boolean = false,
    val isQuotedSpeech: Boolean = false,
    val diagnostics: List<String> = emptyList()
)

/**
 * محاكي اختبار القواعد محلياً (Rule Simulation Sandbox).
 * يسمح باختبار مطابقة القواعد وتوليد الردود والـ Spintax بأمان تام بدون إرسال رسائل حقيقية أو تعديل دفتر الهاتف.
 */
@Singleton
class SimulateRuleExecutionUseCase @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val matchRuleUseCase: MatchRuleUseCase
) {
    suspend fun execute(testMessage: String, testSenderPhone: String = "+967770000000", testSenderName: String? = null): SimulationResult {
        val trace = mutableListOf<String>()
        val trimmedText = testMessage.trim()

        trace.add("بدء محاكاة الرسالة: \"$trimmedText\" من الرقم: $testSenderPhone")

        // 1. فحص درع النفي والكتم الصريح
        val isNegated = StrictNegationAnalyzer.isNegated(trimmedText)
        if (isNegated) {
            trace.add("⚠️ تم رصد أسلوب نفي أو كتم صريح في النص (StrictNegationAnalyzer)")
            return SimulationResult(
                isMatched = false,
                isNegated = true,
                diagnostics = trace + "تم إلغاء المطابقة تلقائياً بسبب كشف النفي."
            )
        }

        // 2. فحص درع الكلام المنقول أو السرد غير المباشر
        val isQuoted = QuotedSpeechDetector.isQuotedOrIndirect(trimmedText)
        if (isQuoted) {
            trace.add("⚠️ تم رصد كلام منقول أو سرد غير مباشر (QuotedSpeechDetector)")
            return SimulationResult(
                isMatched = false,
                isQuotedSpeech = true,
                diagnostics = trace + "تم تجاهل المطابقة لأن النص منقول أو مقتبس."
            )
        }

        // 3. جلب القواعد المفعلة مرتبة بالأولوية
        val rules = try {
            ruleRepository.getEnabledRules().first().sortedBy { it.priority }
        } catch (e: Exception) {
            trace.add("خطأ في جلب القواعد: ${e.message}")
            emptyList()
        }

        trace.add("تم العثور على ${rules.size} قاعدة مفعلة للفحص.")

        // 4. فحص القواعد بالترتيب
        for (rule in rules) {
            val matches = matchRuleUseCase.matches(rule, trimmedText)
            if (matches) {
                trace.add("✅ تطابقت القاعدة: [${rule.name}] (أولوية: ${rule.priority}) بنمط [${rule.patternType}]")

                var finalReply: String? = null
                var extractedName: String? = null

                for (action in rule.actions) {
                    when (action) {
                        is RuleAction.SendReply -> {
                            val replyOptions = action.message.lines().map { it.trim() }.filter { it.isNotBlank() }
                            val template = replyOptions.firstOrNull() ?: action.message
                            finalReply = SpintaxEngine.process(
                                template = template,
                                recipientName = testSenderName ?: "أخي الكريم",
                                recipientPhone = testSenderPhone
                            )
                            trace.add("توليد الرد الآلي عبر محرك Spintax بنجاح.")
                        }
                        is RuleAction.SaveContactAndReply -> {
                            val keywords = rule.patternValue.split(',', '،', '\n').map { it.trim() }.filter { it.isNotBlank() }
                            extractedName = NameExtractorHelper.extractName(
                                text = trimmedText,
                                customKeywords = keywords,
                                fallbackSenderName = testSenderName ?: ""
                            )
                            trace.add("محاكاة استخراج الاسم: ${extractedName ?: "لم يتم العثور على اسم"}")

                            val template = action.replyMessage.ifBlank { "تم حفظك باسم {name} بنجاح ✅" }
                            finalReply = SpintaxEngine.process(
                                template = template,
                                recipientName = extractedName ?: "أخي الكريم",
                                recipientPhone = testSenderPhone
                            )
                            trace.add("توليد رد حفظ جهة الاتصال.")
                        }
                    }
                }

                return SimulationResult(
                    isMatched = true,
                    matchedRuleName = rule.name,
                    matchedRulePriority = rule.priority,
                    patternType = rule.patternType.name,
                    patternValue = rule.patternValue,
                    simulatedReply = finalReply,
                    extractedName = extractedName,
                    diagnostics = trace
                )
            }
        }

        trace.add("❌ لم تتطابق أي قاعدة مفعلة مع نص الرسالة.")
        return SimulationResult(
            isMatched = false,
            diagnostics = trace
        )
    }
}
