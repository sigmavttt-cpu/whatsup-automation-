package com.whatsup.automation.domain.nlp

import com.whatsup.automation.domain.util.ArabicMorphologyHelper

/**
 * كاشف الغضب والتصعيد والشكاوى الحادة (Dialectal Arabic Frustration & Escalation Detector).
 * 
 * يحلل رسائل العملاء الواردة ويرصد:
 * 1. عبارات الاستياء والتأخير والغضب بمختلف اللهجات العربية (يمنية، خليجية، مصرية، شامية).
 * 2. تكرار علامات الاستفهام والتعجب المتتالية الدالة على نفاد الصبر.
 * 3. التهديد بالشكاوى أو الاتهام بالنصب والإهمال.
 * 
 * الهدف: حماية النظام من إرسال ردود آلية تسويقية أو باردة لعميل مستاء،
 * وتوجيه تنبيه عاجل للتدخل البشري الفوري.
 */
object ArabicFrustrationDetector {

    data class FrustrationAnalysisResult(
        val frustrationScore: Float,
        val isHighFrustration: Boolean,
        val detectedReasons: List<String>,
        val explanation: String
    )

    // كلمات وعبارات تدل على غضب شديد أو تهديد (وزن عالي: 0.50 - 0.70)
    private val SEVERE_FRUSTRATION_KEYWORDS = listOf(
        "نصابين", "نصب", "حرامية", "سرقة", "سارقين", "تسرقوا", "نهب",
        "اشتكي عليكم", "اشتكيتكم", "ارفع شكوى", "شرطة", "المحكمة", "حماية المستهلك",
        "ما عندكم ذمة", "ما عندكم ضمير", "قلة ذوق", "قلة حياء", "حسبي الله",
        "خدمة زفت", "خدمة فاشلة", "تطبيق زفت", "اسوء معاملة", "اسوء خدمة", "يا نصابين",
        "فاشلة", "فاشل", "زفت", "خدمتكم فاشلة", "خدمتكم زفت"
    )

    // عبارات تذمر وتأخير وإهمال (وزن متوسط: 0.30 - 0.45)
    private val MODERATE_COMPLAINT_KEYWORDS = listOf(
        "وين طلبي", "وين شحنتي", "وين الشحنة", "تأخرتوا", "تاخرتوا", "ليش التأخير", "ليش التاخير",
        "صار لي أسبوع", "صارلي اسبوع", "صارلي شهر", "صار لي شهر", "صارلي فترة", "وينكم",
        "ليش ما تردوا", "ليش ما تردون", "ليش ماتردوا", "ما تردوا", "ما تردون", "مافي رد",
        "تتجاهلوني", "تجاهل", "ما ترد", "ردوا علي", "رد ياخي", "ردوا يا جماعة", "وين الناس",
        "ألووو", "الوووو", "الو؟؟", "صار لنا ننتظر", "ملينا", "تعبنا"
    )

    // أنماط علامات الترقيم الانفعالية
    private val REPEATED_QUESTION_REGEX = Regex("""[؟?]{2,}""")
    private val REPEATED_EXCLAMATION_REGEX = Regex("""[!]{2,}""")
    private val MIXED_PUNCTUATION_REGEX = Regex("""([؟?!]){3,}""")

    /**
     * فحص وتحليل درجة الاستفزاز والغضب في النص.
     */
    fun analyze(rawText: String): FrustrationAnalysisResult {
        if (rawText.isBlank()) {
            return FrustrationAnalysisResult(0.0f, false, emptyList(), "نص فارغ")
        }

        val norm = ArabicMorphologyHelper.normalize(rawText).trim()
        val detectedReasons = mutableListOf<String>()
        var score = 0.0f

        // 1. فحص الكلمات الحادة والشديدة
        for (kw in SEVERE_FRUSTRATION_KEYWORDS) {
            val normalizedKw = ArabicMorphologyHelper.normalize(kw)
            if (norm.contains(normalizedKw)) {
                score += 0.60f
                detectedReasons.add("عبارة غضب حادة: \"$kw\"")
                break
            }
        }

        // 2. فحص عبارات التذمر والشكاوى من التأخير (تراكم الشكاوى يرفع درجة الاستياء)
        var moderateMatches = 0
        for (kw in MODERATE_COMPLAINT_KEYWORDS) {
            val normalizedKw = ArabicMorphologyHelper.normalize(kw)
            if (norm.contains(normalizedKw)) {
                score += if (moderateMatches == 0) 0.35f else 0.25f
                detectedReasons.add("شكوى تذمر أو تأخير: \"$kw\"")
                moderateMatches++
                if (moderateMatches >= 2) break
            }
        }

        // 3. فحص انفجار علامات الترقيم (الانفعال البصري)
        if (MIXED_PUNCTUATION_REGEX.containsMatchIn(rawText)) {
            score += 0.25f
            detectedReasons.add("علامات تعجب واستفهام انفعالية متتالية")
        } else {
            if (REPEATED_QUESTION_REGEX.containsMatchIn(rawText)) {
                score += 0.20f
                detectedReasons.add("تكرار علامات الاستفهام")
            }
            if (REPEATED_EXCLAMATION_REGEX.containsMatchIn(rawText)) {
                score += 0.15f
                detectedReasons.add("تكرار علامات التعجب")
            }
        }

        // حصر النتيجة بين 0.0 و 1.0
        val finalScore = score.coerceIn(0.0f, 1.0f)
        val isHigh = finalScore >= 0.55f

        val explanation = when {
            finalScore >= 0.75f -> "مستوى غضب شديد — يتطلب تدخلاً بشرياً عاجلاً فورياً"
            finalScore >= 0.55f -> "مستوى تذمر ملحوظ — يُوصى بكتم الردود التسويقية الآلية"
            finalScore > 0.20f -> "استفسار يحمل نبرة تعجل خفيفة"
            else -> "نبرة محايدة أو إيجابية"
        }

        return FrustrationAnalysisResult(
            frustrationScore = finalScore,
            isHighFrustration = isHigh,
            detectedReasons = detectedReasons,
            explanation = explanation
        )
    }
}
