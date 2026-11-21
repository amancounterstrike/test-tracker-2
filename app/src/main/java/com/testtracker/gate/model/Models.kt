package com.testtracker.gate.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt

@Immutable
data class SpacingStage(
    val code: String,
    val offsetDays: Int
)

val SPACING_SCHEDULE: List<SpacingStage> = listOf(
    SpacingStage("R0", 0),
    SpacingStage("R1", 3),
    SpacingStage("R2", 8),
    SpacingStage("R3", 16),
    SpacingStage("R4", 30),
    SpacingStage("R5", 45),
    SpacingStage("R6", 60),
    SpacingStage("R7", 75)
)

@Immutable
data class ErrorCategory(
    val code: String,
    val label: String,
    val colorHex: Long
) {
    val color: Color get() = Color(colorHex)
}

val ERROR_CATEGORIES: List<ErrorCategory> = listOf(
    ErrorCategory("C", "Concept", 0xFF6366F1),
    ErrorCategory("F", "Formula", 0xFFA855F7),
    ErrorCategory("A", "Application", 0xFF3B82F6),
    ErrorCategory("E", "Calculation error", 0xFFF59E0B),
    ErrorCategory("T", "Took too much time", 0xFFF43F5E)
)

fun errorCategoryFor(code: String): ErrorCategory {
    return ERROR_CATEGORIES.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }
        ?: ERROR_CATEGORIES.first()
}

val SUBJECT_COLORS: Map<String, Long> = mapOf(
    "Structural Engineering" to 0xFF2563EB,
    "Environmental Engineering" to 0xFF8B5CF6,
    "Engineering Mathematics" to 0xFFDC2626,
    "Geotechnical Engineering" to 0xFF0D9488,
    "Transportation Engineering" to 0xFFD97706,
    "Engineering Mechanics" to 0xFF4338CA,
    "General Aptitude" to 0xFFDB2777,
    "RCC & Prestressed Concrete" to 0xFF059669,
    "Fluid Mechanics & Hydraulics" to 0xFF0891B2,
    "Geomatics Engineering (Surveying)" to 0xFF854D0E,
    "Construction Materials & Management" to 0xFF65A30D,
    "Steel Structures" to 0xFF475569
)

val SUGGESTED_SUBJECTS: List<String> = SUBJECT_COLORS.keys.toList() + listOf(
    "Water Resources Engineering",
    "Hydrology & Irrigation"
)

private val FALLBACK_SUBJECT_PALETTE = listOf(
    0xFF2563EB, 0xFF8B5CF6, 0xFFDC2626, 0xFF0D9488,
    0xFFD97706, 0xFF4338CA, 0xFFDB2777, 0xFF059669,
    0xFF0891B2, 0xFF854D0E, 0xFF65A30D, 0xFF475569
)

fun subjectColorFor(subject: String): Color {
    val trimmed = subject.trim()
    SUBJECT_COLORS[trimmed]?.let { return Color(it) }
    SUBJECT_COLORS.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) }?.let {
        return Color(it.value)
    }
    val hash = abs(trimmed.lowercase(Locale.ROOT).hashCode())
    return Color(FALLBACK_SUBJECT_PALETTE[hash % FALLBACK_SUBJECT_PALETTE.size])
}

@Immutable
data class TestRecord(
    val id: String = UUID.randomUUID().toString(),
    val testNumber: String,
    val subject: String,
    val topic: String,
    val testDate: String, // YYYY-MM-DD
    val totalMarks: Double? = null,
    val marksScored: Double? = null,
    val correctCount: Int? = null,
    val incorrectCount: Int? = null,
    val skippedCount: Int? = null,
    val timeTakenMinutes: Int? = null,
    val notes: String = ""
) {
    val accuracyPercent: Int?
        get() = computeAccuracy(correctCount, incorrectCount, skippedCount)

    val scorePercent: Int?
        get() = if (totalMarks != null && totalMarks > 0.0 && marksScored != null) {
            ((marksScored / totalMarks) * 100.0).roundToInt()
        } else null
}

@Immutable
data class QuestionRecord(
    val id: String = UUID.randomUUID().toString(),
    val testId: String,
    val questionNumber: String,
    val errorCode: String, // C, F, A, E, T
    val topicTag: String = "",
    val notes: String = "",
    val createdDate: String = todayIso()
)

@Immutable
data class RevisionRecord(
    val id: String = UUID.randomUUID().toString(),
    val testId: String,
    val stageCode: String, // R0..R7
    val scheduledDate: String, // YYYY-MM-DD (never shifts)
    val isCompleted: Boolean = false,
    val completedDate: String? = null,
    val difficulty: String? = null, // Easy, Medium, Hard
    val notes: String = ""
)

enum class RevisionStatus {
    COMPLETED,
    OVERDUE,
    TODAY,
    UPCOMING
}

@Immutable
data class RevisionWithTest(
    val revision: RevisionRecord,
    val test: TestRecord,
    val status: RevisionStatus
)

@Immutable
data class AppSettings(
    val darkMode: Boolean = false,
    val examTitle: String = "GATE Test Series 2027 🏆",
    val branchSubtitle: String = "GATE Civil Engineering (CE)"
)

fun computeAccuracy(correct: Int?, incorrect: Int?, skipped: Int?): Int? {
    val c = correct ?: return null
    val w = incorrect ?: 0
    val s = skipped ?: 0
    val total = c + w + s
    if (total <= 0) return null
    return ((c.toDouble() / total.toDouble()) * 100.0).roundToInt()
}

private val ISO_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private val DISPLAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
private val SHORT_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)
private val BANNER_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH)

fun todayIso(): String = LocalDate.now().format(ISO_FORMATTER)

fun todayBannerDate(): String = LocalDate.now().format(BANNER_FORMATTER)

fun parseLocalDate(isoDate: String): LocalDate {
    return try {
        LocalDate.parse(isoDate.take(10), ISO_FORMATTER)
    } catch (_: Exception) {
        LocalDate.now()
    }
}

fun addDaysIso(isoDate: String, days: Int): String {
    return parseLocalDate(isoDate).plusDays(days.toLong()).format(ISO_FORMATTER)
}

fun formatDisplayDate(isoDate: String): String {
    return try {
        parseLocalDate(isoDate).format(DISPLAY_FORMATTER)
    } catch (_: Exception) {
        isoDate
    }
}

fun formatShortDate(isoDate: String): String {
    return try {
        parseLocalDate(isoDate).format(SHORT_FORMATTER)
    } catch (_: Exception) {
        isoDate
    }
}

fun daysBetweenIso(fromIso: String, toIso: String): Long {
    return ChronoUnit.DAYS.between(parseLocalDate(fromIso), parseLocalDate(toIso))
}

fun revisionStatusFor(revision: RevisionRecord, today: String = todayIso()): RevisionStatus {
    if (revision.isCompleted) return RevisionStatus.COMPLETED
    val diff = daysBetweenIso(today, revision.scheduledDate)
    return when {
        diff < 0L -> RevisionStatus.OVERDUE
        diff == 0L -> RevisionStatus.TODAY
        else -> RevisionStatus.UPCOMING
    }
}

fun generateRevisionsForTest(test: TestRecord): List<RevisionRecord> {
    return SPACING_SCHEDULE.map { stage ->
        val scheduled = addDaysIso(test.testDate, stage.offsetDays)
        val isR0 = stage.code == "R0"
        RevisionRecord(
            id = "${test.id}_${stage.code}",
            testId = test.id,
            stageCode = stage.code,
            scheduledDate = scheduled,
            isCompleted = isR0,
            completedDate = if (isR0) test.testDate else null,
            difficulty = null,
            notes = if (isR0) "Initial test attempt" else ""
        )
    }
}

data class SampleDataset(
    val tests: List<TestRecord>,
    val revisions: List<RevisionRecord>,
    val questions: List<QuestionRecord>
)

fun buildSampleGateCeDataset(): SampleDataset {
    val today = LocalDate.now()
    fun daysAgo(d: Long): String = today.minusDays(d).format(ISO_FORMATTER)

    val t1 = TestRecord(
        id = "sample-ce-01",
        testNumber = "CE-Mock-01",
        subject = "Geotechnical Engineering",
        topic = "Soil Mechanics & Foundations",
        testDate = daysAgo(18),
        totalMarks = 100.0,
        marksScored = 68.5,
        correctCount = 42,
        incorrectCount = 14,
        skippedCount = 9,
        timeTakenMinutes = 170,
        notes = "Strong in consolidation & effective stress; revise Terzaghi bearing capacity factors."
    )
    val t2 = TestRecord(
        id = "sample-ce-02",
        testNumber = "CE-Topic-02",
        subject = "Structural Engineering",
        topic = "Determinate & Indeterminate Trusses",
        testDate = daysAgo(16),
        totalMarks = 50.0,
        marksScored = 38.0,
        correctCount = 21,
        incorrectCount = 6,
        skippedCount = 3,
        timeTakenMinutes = 85,
        notes = "Moment distribution signs need extra care under time pressure."
    )
    val t3 = TestRecord(
        id = "sample-ce-03",
        testNumber = "CE-Topic-03",
        subject = "Fluid Mechanics & Hydraulics",
        topic = "Open Channel Flow & Hydraulic Jump",
        testDate = daysAgo(8),
        totalMarks = 50.0,
        marksScored = 31.5,
        correctCount = 18,
        incorrectCount = 9,
        skippedCount = 3,
        timeTakenMinutes = 90,
        notes = "Missed specific energy curve edge case and Manning's n unit conversion."
    )
    val t4 = TestRecord(
        id = "sample-ce-04",
        testNumber = "CE-Mock-04",
        subject = "Engineering Mathematics",
        topic = "Linear Algebra & Calculus",
        testDate = daysAgo(5),
        totalMarks = 30.0,
        marksScored = 25.0,
        correctCount = 14,
        incorrectCount = 3,
        skippedCount = 1,
        timeTakenMinutes = 55,
        notes = "Eigenvalue properties were fast; minor sign slip in triple integral."
    )
    val t5 = TestRecord(
        id = "sample-ce-05",
        testNumber = "CE-Topic-05",
        subject = "Environmental Engineering",
        topic = "Water Treatment & BOD Kinetics",
        testDate = daysAgo(3),
        totalMarks = 50.0,
        marksScored = 40.0,
        correctCount = 23,
        incorrectCount = 4,
        skippedCount = 3,
        timeTakenMinutes = 80,
        notes = "Excellent accuracy on rapid sand filter & sedimentation tank overflow rate."
    )
    val t6 = TestRecord(
        id = "sample-ce-06",
        testNumber = "CE-Topic-06",
        subject = "Transportation Engineering",
        topic = "Highway Geometric Design & Traffic Flow",
        testDate = daysAgo(1),
        totalMarks = 50.0,
        marksScored = 34.0,
        correctCount = 19,
        incorrectCount = 7,
        skippedCount = 4,
        timeTakenMinutes = 85,
        notes = "Review superelevation equilibrium formula and Greenshields jam density."
    )

    val tests = listOf(t1, t2, t3, t4, t5, t6)
    val revisions = tests.flatMap { test ->
        generateRevisionsForTest(test).map { rev ->
            when {
                test.id == "sample-ce-01" && rev.stageCode in listOf("R1", "R2") ->
                    rev.copy(
                        isCompleted = true,
                        completedDate = rev.scheduledDate,
                        difficulty = if (rev.stageCode == "R1") "Hard" else "Medium",
                        notes = "Re-solved bearing capacity numericals"
                    )
                test.id == "sample-ce-02" && rev.stageCode == "R1" ->
                    rev.copy(
                        isCompleted = true,
                        completedDate = rev.scheduledDate,
                        difficulty = "Easy",
                        notes = "Quick formula recap"
                    )
                test.id == "sample-ce-03" && rev.stageCode == "R1" ->
                    rev.copy(
                        isCompleted = true,
                        completedDate = rev.scheduledDate,
                        difficulty = "Medium",
                        notes = "Reviewed sequent depth relation"
                    )
                else -> rev
            }
        }
    }

    val questions = listOf(
        QuestionRecord("sq-1", t1.id, "Q14", "C", "Consolidation", "Confused Cv with mv in time factor formula", t1.testDate),
        QuestionRecord("sq-2", t1.id, "Q27", "F", "Bearing Capacity", "Forgot water table correction factor Rw", t1.testDate),
        QuestionRecord("sq-3", t1.id, "Q49", "E", "Shear Strength", "Calculator rounding error in Mohr circle angle", t1.testDate),
        QuestionRecord("sq-4", t2.id, "Q08", "A", "Indeterminate Trusses", "Missed redundant member compatibility sign", t2.testDate),
        QuestionRecord("sq-5", t2.id, "Q19", "T", "Slope Deflection", "Spent 8 minutes setting up 3-joint matrix", t2.testDate),
        QuestionRecord("sq-6", t3.id, "Q14", "F", "Hydraulic Jump", "Repeated Q14 slip: used wrong Froude number power", t3.testDate),
        QuestionRecord("sq-7", t3.id, "Q22", "E", "Open Channel", "Unit conversion error for discharge per unit width", t3.testDate),
        QuestionRecord("sq-8", t4.id, "Q11", "C", "Vector Calculus", "Misapplied Stokes theorem boundary orientation", t4.testDate),
        QuestionRecord("sq-9", t6.id, "Q16", "A", "Geometric Design", "Used SSD instead of OSD for two-way undivided road", t6.testDate)
    )

    return SampleDataset(tests = tests, revisions = revisions, questions = questions)
}
