package com.testtracker.gate.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.testtracker.gate.data.TestTrackerRepository
import com.testtracker.gate.model.AppSettings
import com.testtracker.gate.model.QuestionRecord
import com.testtracker.gate.model.RevisionRecord
import com.testtracker.gate.model.RevisionStatus
import com.testtracker.gate.model.RevisionWithTest
import com.testtracker.gate.model.TestRecord
import com.testtracker.gate.model.revisionStatusFor
import com.testtracker.gate.model.todayIso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.roundToInt

/**
 * All derived lists and KPIs are precomputed off the UI thread so every frame
 * during 120Hz scrolling performs zero filtering, sorting, or date parsing.
 */
@Immutable
data class TrackerUiState(
    val isLoading: Boolean = true,
    val tests: List<TestRecord> = emptyList(),
    val revisions: List<RevisionRecord> = emptyList(),
    val questions: List<QuestionRecord> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val snackbarMessage: String? = null,
    val testMap: Map<String, TestRecord> = emptyMap(),
    val trackableRevisions: List<RevisionWithTest> = emptyList(),
    val dueTodayRevisions: List<RevisionWithTest> = emptyList(),
    val overdueRevisions: List<RevisionWithTest> = emptyList(),
    val upcomingRevisions: List<RevisionWithTest> = emptyList(),
    val completedRevisions: List<RevisionWithTest> = emptyList(),
    val averageScorePercent: Int? = null,
    val averageAccuracyPercent: Int? = null,
    val revisionCompletionRate: Int = 0,
    val repeatedQuestionNumbers: Set<String> = emptySet(),
    val distinctSubjects: List<String> = emptyList()
)

private fun buildComputedState(
    tests: List<TestRecord>,
    revisions: List<RevisionRecord>,
    questions: List<QuestionRecord>,
    settings: AppSettings,
    snackbarMessage: String?
): TrackerUiState {
    val today = todayIso()
    val tMap = tests.associateBy { it.id }

    val trackable = ArrayList<RevisionWithTest>(revisions.size)
    val dueToday = ArrayList<RevisionWithTest>()
    val overdue = ArrayList<RevisionWithTest>()
    val upcoming = ArrayList<RevisionWithTest>()
    val completed = ArrayList<RevisionWithTest>()

    for (rev in revisions) {
        if (rev.stageCode == "R0") continue
        val test = tMap[rev.testId] ?: continue
        val status = revisionStatusFor(rev, today)
        val item = RevisionWithTest(rev, test, status)
        trackable.add(item)
        when (status) {
            RevisionStatus.TODAY -> dueToday.add(item)
            RevisionStatus.OVERDUE -> overdue.add(item)
            RevisionStatus.UPCOMING -> upcoming.add(item)
            RevisionStatus.COMPLETED -> completed.add(item)
        }
    }

    val scored = tests.mapNotNull { it.scorePercent }
    val avgScore = if (scored.isEmpty()) null else scored.average().roundToInt()

    val acc = tests.mapNotNull { it.accuracyPercent }
    val avgAcc = if (acc.isEmpty()) null else acc.average().roundToInt()

    val revRate = if (trackable.isEmpty()) 0 else {
        ((completed.size.toDouble() / trackable.size.toDouble()) * 100.0).roundToInt()
    }

    val repeated = questions
        .groupBy { it.questionNumber.trim().uppercase() }
        .filter { it.value.map { q -> q.testId }.distinct().size > 1 }
        .keys

    val subjects = tests.map { it.subject }.distinct().sorted()

    return TrackerUiState(
        isLoading = false,
        tests = tests,
        revisions = revisions,
        questions = questions,
        settings = settings,
        snackbarMessage = snackbarMessage,
        testMap = tMap,
        trackableRevisions = trackable,
        dueTodayRevisions = dueToday,
        overdueRevisions = overdue,
        upcomingRevisions = upcoming,
        completedRevisions = completed,
        averageScorePercent = avgScore,
        averageAccuracyPercent = avgAcc,
        revisionCompletionRate = revRate,
        repeatedQuestionNumbers = repeated,
        distinctSubjects = subjects
    )
}

class TrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TestTrackerRepository(application)

    private val _uiState = MutableStateFlow(TrackerUiState())
    val uiState: StateFlow<TrackerUiState> = _uiState.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll(message: String? = null) {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                val tests = repository.getAllTests()
                val revisions = repository.getAllRevisions()
                val questions = repository.getAllQuestions()
                val settings = repository.getSettings()
                buildComputedState(tests, revisions, questions, settings, message)
            }
            _uiState.value = loaded
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    fun showMessage(msg: String) {
        _uiState.value = _uiState.value.copy(snackbarMessage = msg)
    }

    fun toggleDarkMode() {
        val updated = _uiState.value.settings.copy(darkMode = !_uiState.value.settings.darkMode)
        _uiState.value = _uiState.value.copy(settings = updated)
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveSettings(updated)
        }
    }

    fun saveTest(test: TestRecord, isEditing: Boolean, previousDate: String? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (isEditing) {
                    repository.updateTest(test, dateChanged = previousDate != null && previousDate != test.testDate)
                } else {
                    repository.addTestWithRevisions(test)
                }
            }
            refreshAll(if (isEditing) "Updated ${test.testNumber}" else "Added ${test.testNumber} with R0–R7 schedule")
        }
    }

    fun deleteTest(test: TestRecord) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteTest(test.id)
            }
            refreshAll("Deleted ${test.testNumber}")
        }
    }

    fun quickCompleteRevision(revision: RevisionRecord) {
        val updated = revision.copy(
            isCompleted = !revision.isCompleted,
            completedDate = if (!revision.isCompleted) todayIso() else null,
            difficulty = if (!revision.isCompleted) revision.difficulty ?: "Medium" else null
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateRevision(updated)
            }
            refreshAll(
                if (updated.isCompleted) "${updated.stageCode} marked Done"
                else "${updated.stageCode} reopened"
            )
        }
    }

    fun completeRevisionDetailed(
        revision: RevisionRecord,
        completedDate: String,
        difficulty: String,
        notes: String
    ) {
        val updated = revision.copy(
            isCompleted = true,
            completedDate = completedDate,
            difficulty = difficulty,
            notes = notes
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.updateRevision(updated)
            }
            refreshAll("${updated.stageCode} completed ($difficulty)")
        }
    }

    fun addSingleQuestion(
        testId: String,
        questionNumber: String,
        errorCode: String,
        topicTag: String,
        notes: String
    ) {
        val q = QuestionRecord(
            id = UUID.randomUUID().toString(),
            testId = testId,
            questionNumber = questionNumber.trim().ifBlank { "Q1" },
            errorCode = errorCode.trim().uppercase().take(1),
            topicTag = topicTag.trim(),
            notes = notes.trim(),
            createdDate = todayIso()
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.addQuestion(q)
            }
            refreshAll("Logged ${q.questionNumber} (${q.errorCode})")
        }
    }

    fun addBulkQuestions(testId: String, bulkText: String, defaultTopic: String) {
        val lines = bulkText.lines()
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val parsed = mutableListOf<QuestionRecord>()
        val validCodes = setOf("C", "F", "A", "E", "T")
        val today = todayIso()

        lines.forEach { entry ->
            val parts = entry.split(":", "-", " ")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            if (parts.isNotEmpty()) {
                val qNum = if (parts[0].startsWith("Q", ignoreCase = true)) parts[0].uppercase() else "Q${parts[0]}"
                val codeCandidate = parts.getOrNull(1)?.uppercase()?.take(1) ?: "C"
                val code = if (codeCandidate in validCodes) codeCandidate else "C"
                val extraNotes = if (parts.size > 2) parts.drop(2).joinToString(" ") else ""
                parsed.add(
                    QuestionRecord(
                        id = UUID.randomUUID().toString(),
                        testId = testId,
                        questionNumber = qNum,
                        errorCode = code,
                        topicTag = defaultTopic.trim(),
                        notes = extraNotes,
                        createdDate = today
                    )
                )
            }
        }

        if (parsed.isEmpty()) {
            showMessage("Enter lines like Q3: C, Q8: F")
            return
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.addQuestionsBulk(parsed)
            }
            refreshAll("Added ${parsed.size} questions")
        }
    }

    fun deleteQuestion(questionId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteQuestion(questionId)
            }
            refreshAll("Question removed")
        }
    }

    fun loadSampleGateData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.loadSampleData()
            }
            refreshAll("Loaded 6 sample GATE CE tests & 9 questions")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.clearAllData()
            }
            refreshAll("All data cleared")
        }
    }

    suspend fun exportJsonString(): String = withContext(Dispatchers.IO) {
        repository.exportToJson()
    }

    suspend fun exportCsvString(): String = withContext(Dispatchers.IO) {
        repository.exportToCsv()
    }

    fun importJsonString(json: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                repository.importFromJson(json)
            }
            result.fold(
                onSuccess = { count -> refreshAll("Imported $count tests successfully") },
                onFailure = { err -> showMessage("Import failed: ${err.message ?: "Invalid JSON"}") }
            )
        }
    }
}
