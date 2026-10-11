package com.whatsup.automation.presentation.diagnostics

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * نموذج العنصر البصري المفحوص (UI Element Bounding Node).
 */
data class UiElementNode(
    val id: String,
    val name: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val isClickable: Boolean = true,
    val isEnabled: Boolean = true,
    val role: String? = null,
    val zIndex: Float = 0f
) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
    val area: Int get() = width * height

    fun intersects(other: UiElementNode): Boolean {
        return left < other.right && right > other.left && top < other.bottom && bottom > other.top
    }
}

/**
 * خطر التداخل بين أزرار اللمس (Touch Overlap Hazard).
 */
data class OverlapHazard(
    val elementA: String,
    val elementB: String,
    val overlapAreaPx: Int,
    val description: String
)

/**
 * مخالفة صغر مساحة اللمس القياسية (48dp Minimum Touch Target).
 */
data class TouchTargetViolation(
    val elementName: String,
    val widthPx: Int,
    val heightPx: Int,
    val description: String
)

/**
 * خطر النقرة الشبحية (Ghost Click Hazard).
 */
data class GhostClickHazard(
    val containerName: String,
    val coveragePercent: Float,
    val description: String
)

/**
 * تقرير فحص الواجهات الشامل بدون صور أو فيديو للذكاء الاصطناعي والمطور.
 */
data class LayoutAuditReport(
    val timestamp: Long = System.currentTimeMillis(),
    val totalElements: Int,
    val clickableCount: Int,
    val overlapHazards: List<OverlapHazard>,
    val touchTargetViolations: List<TouchTargetViolation>,
    val ghostClickHazards: List<GhostClickHazard>
) {
    val hasCriticalIssues: Boolean
        get() = overlapHazards.isNotEmpty() || ghostClickHazards.isNotEmpty()

    val score: Int
        get() {
            var s = 100
            s -= overlapHazards.size * 25
            s -= ghostClickHazards.size * 20
            s -= touchTargetViolations.size * 5
            return s.coerceIn(0, 100)
        }
}

/**
 * محرك الرؤية الهيكلية وفحص الأزرار واللمس النصي (Headless AI UI Auditor).
 * يُمكّن الـ AI Agent والمطور من فحص كافة أزرار وتخطيطات الشاشة رياضياً وبدقة البكسل
 * دون الحاجة لأي صور أو فيديو، مع كشف فوري للصناديق الشفافة والنقرات المتداخلة.
 */
object ComposeLayoutAuditor {

    private const val TAG = "ComposeLayoutAuditor"
    private const val MIN_TOUCH_SIZE_PX = 120 // ~48dp على شاشات xhdpi/xxhdpi

    /**
     * فحص قائمة العناصر الحالية وكشف أي أخطاء تداخل أو نقرات شبحية.
     */
    fun audit(
        elements: List<UiElementNode>,
        screenWidthPx: Int = 1080,
        screenHeightPx: Int = 2400
    ): LayoutAuditReport {
        val clickables = elements.filter { it.isClickable && it.isEnabled }
        val overlaps = mutableListOf<OverlapHazard>()
        val violations = mutableListOf<TouchTargetViolation>()
        val ghostClicks = mutableListOf<GhostClickHazard>()

        val totalScreenArea = (screenWidthPx * screenHeightPx).coerceAtLeast(1)

        // 1. فحص التداخل الهندسي بين الأزرار المتجاورة
        for (i in clickables.indices) {
            val a = clickables[i]

            // فحص صغر مساحة اللمس
            if (a.width < MIN_TOUCH_SIZE_PX || a.height < MIN_TOUCH_SIZE_PX) {
                violations.add(
                    TouchTargetViolation(
                        elementName = a.name,
                        widthPx = a.width,
                        heightPx = a.height,
                        description = "مساحة زر (${a.name}) صغيرة جداً (${a.width}x${a.height}px) وأقل من الحد الأدنى 48dp."
                    )
                )
            }

            // فحص تغطية الحاويات الشفافة الكبيرة (Ghost Click)
            val coverage = (a.area.toFloat() / totalScreenArea.toFloat()) * 100f
            if (coverage >= 50f && (a.role == null || a.role == "Container")) {
                ghostClicks.add(
                    GhostClickHazard(
                        containerName = a.name,
                        coveragePercent = coverage,
                        description = "الحاوية (${a.name}) تغطي ${coverage.toInt()}% من الشاشة وتمتلك clickable، قد تبتلع نقرات الأزرار التحتية!"
                    )
                )
            }

            // فحص التقاطع مع بقية الأزرار
            for (j in i + 1 until clickables.size) {
                val b = clickables[j]
                if (a.intersects(b) && a.name != b.name) {
                    val overlapW = (minOf(a.right, b.right) - maxOf(a.left, b.left)).coerceAtLeast(0)
                    val overlapH = (minOf(a.bottom, b.bottom) - maxOf(a.top, b.top)).coerceAtLeast(0)
                    val overlapArea = overlapW * overlapH

                    if (overlapArea > 100) {
                        overlaps.add(
                            OverlapHazard(
                                elementA = a.name,
                                elementB = b.name,
                                overlapAreaPx = overlapArea,
                                description = "تداخل أزرار: الزر (${a.name}) يتقاطع مع الزر (${b.name}) بمساحة $overlapArea بكسل!"
                            )
                        )
                    }
                }
            }
        }

        return LayoutAuditReport(
            totalElements = elements.size,
            clickableCount = clickables.size,
            overlapHazards = overlaps,
            touchTargetViolations = violations,
            ghostClickHazards = ghostClicks
        )
    }

    /**
     * تصدير التقرير إلى تنسيق JSON نظيف وخفيف للذكاء الاصطناعي.
     */
    fun toJson(report: LayoutAuditReport): String {
        val root = JSONObject().apply {
            put("timestamp", report.timestamp)
            put("healthScore", "${report.score}/100")
            put("totalElements", report.totalElements)
            put("clickableElements", report.clickableCount)
            put("hasCriticalIssues", report.hasCriticalIssues)

            val overlapsArray = JSONArray()
            report.overlapHazards.forEach {
                overlapsArray.put(JSONObject().apply {
                    put("elementA", it.elementA)
                    put("elementB", it.elementB)
                    put("overlapAreaPx", it.overlapAreaPx)
                    put("issue", it.description)
                })
            }
            put("overlapHazards", overlapsArray)

            val violationsArray = JSONArray()
            report.touchTargetViolations.forEach {
                violationsArray.put(JSONObject().apply {
                    put("element", it.elementName)
                    put("dimensions", "${it.widthPx}x${it.heightPx}px")
                    put("issue", it.description)
                })
            }
            put("touchTargetViolations", violationsArray)

            val ghostsArray = JSONArray()
            report.ghostClickHazards.forEach {
                ghostsArray.put(JSONObject().apply {
                    put("container", it.containerName)
                    put("screenCoverage", "${it.coveragePercent.toInt()}%")
                    put("issue", it.description)
                })
            }
            put("ghostClickHazards", ghostsArray)
        }
        return root.toString(2)
    }

    /**
     * حفظ التقرير محلياً في الهاتف.
     */
    fun saveReportLocally(context: Context, report: LayoutAuditReport): File {
        val dir = File(context.filesDir, "dev_diagnostics/ui_audit")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "latest_ui_audit.json")
        file.writeText(toJson(report))
        Log.d(TAG, "UI Layout Audit Report saved to: ${file.absolutePath}")
        return file
    }
}
