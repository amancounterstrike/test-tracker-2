package com.testtracker.gate.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.testtracker.gate.model.AppSettings
import com.testtracker.gate.model.QuestionRecord
import com.testtracker.gate.model.RevisionRecord
import com.testtracker.gate.model.SPACING_SCHEDULE
import com.testtracker.gate.model.TestRecord
import com.testtracker.gate.model.addDaysIso
import com.testtracker.gate.model.buildSampleGateCeDataset
import com.testtracker.gate.model.generateRevisionsForTest
import com.testtracker.gate.model.todayIso
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

class TestTrackerRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context.applicationContext)

    fun getAllTests(): List<TestRecord> {
        val list = mutableListOf<TestRecord>()
        val db = dbHelper.readableDatabase
        db.rawQuery("SELECT * FROM tests ORDER BY test_date DESC, test_number ASC", null).use { c ->
            while (c.moveToNext()) {
                list.add(c.toTestRecord())
            }
        }
        return list
    }

    fun getAllRevisions(): List<RevisionRecord> {
        val list = mutableListOf<RevisionRecord>()
        val db = dbHelper.readableDatabase
        db.rawQuery("SELECT * FROM revisions ORDER BY scheduled_date ASC, stage_code ASC", null).use { c ->
            while (c.moveToNext()) {
                list.add(c.toRevisionRecord())
            }
        }
        return list
    }

    fun getAllQuestions(): List<QuestionRecord> {
        val list = mutableListOf<QuestionRecord>()
        val db = dbHelper.readableDatabase
        db.rawQuery("SELECT * FROM questions ORDER BY created_date DESC, question_number ASC", null).use { c ->
            while (c.moveToNext()) {
                list.add(c.toQuestionRecord())
            }
        }
        return list
    }

    fun getSettings(): AppSettings {
        val db = dbHelper.readableDatabase
        val map = mutableMapOf<String, String>()
        db.rawQuery("SELECT key, value FROM settings", null).use { c ->
            while (c.moveToNext()) {
                map[c.getString(0)] = c.getString(1)
            }
        }
        return AppSettings(
            darkMode = map["darkMode"] == "true",
            examTitle = map["examTitle"] ?: "GATE Test Series 2027 🏆",
            branchSubtitle = map["branchSubtitle"] ?: "GATE Civil Engineering (CE)"
        )
    }

    fun saveSettings(settings: AppSettings) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            putSetting(db, "darkMode", settings.darkMode.toString())
            putSetting(db, "examTitle", settings.examTitle)
            putSetting(db, "branchSubtitle", settings.branchSubtitle)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun putSetting(db: SQLiteDatabase, key: String, value: String) {
        val cv = ContentValues().apply {
            put("key", key)
            put("value", value)
        }
        db.insertWithOnConflict("settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun addTestWithRevisions(test: TestRecord) {
        val db = dbHelper.writableDatabase
        val revisions = generateRevisionsForTest(test)
        db.beginTransaction()
        try {
            db.insertWithOnConflict("tests", null, test.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
            revisions.forEach { rev ->
                db.insertWithOnConflict("revisions", null, rev.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun updateTest(updated: TestRecord, dateChanged: Boolean) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.insertWithOnConflict("tests", null, updated.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
            if (dateChanged) {
                SPACING_SCHEDULE.forEach { stage ->
                    val newScheduled = addDaysIso(updated.testDate, stage.offsetDays)
                    val cv = ContentValues().apply {
                        put("scheduled_date", newScheduled)
                        if (stage.code == "R0") {
                            put("is_completed", 1)
                            put("completed_date", updated.testDate)
                        }
                    }
                    db.update(
                        "revisions",
                        cv,
                        "test_id = ? AND stage_code = ?",
                        arrayOf(updated.id, stage.code)
                    )
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteTest(testId: String) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("questions", "test_id = ?", arrayOf(testId))
            db.delete("revisions", "test_id = ?", arrayOf(testId))
            db.delete("tests", "id = ?", arrayOf(testId))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun updateRevision(revision: RevisionRecord) {
        val db = dbHelper.writableDatabase
        db.insertWithOnConflict("revisions", null, revision.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun addQuestion(question: QuestionRecord) {
        val db = dbHelper.writableDatabase
        db.insertWithOnConflict("questions", null, question.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun addQuestionsBulk(questions: List<QuestionRecord>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            questions.forEach { q ->
                db.insertWithOnConflict("questions", null, q.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun deleteQuestion(questionId: String) {
        val db = dbHelper.writableDatabase
        db.delete("questions", "id = ?", arrayOf(questionId))
    }

    fun clearAllData() {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("questions", null, null)
            db.delete("revisions", null, null)
            db.delete("tests", null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun loadSampleData() {
        val sample = buildSampleGateCeDataset()
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("questions", null, null)
            db.delete("revisions", null, null)
            db.delete("tests", null, null)
            sample.tests.forEach { t ->
                db.insertWithOnConflict("tests", null, t.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
            }
            sample.revisions.forEach { r ->
                db.insertWithOnConflict("revisions", null, r.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
            }
            sample.questions.forEach { q ->
                db.insertWithOnConflict("questions", null, q.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun exportToJson(): String {
        val tests = getAllTests()
        val revisions = getAllRevisions()
        val questions = getAllQuestions()
        val settings = getSettings()

        val root = JSONObject().apply {
            put("appName", "Test Tracker")
            put("exportDate", Instant.now().toString())
            put("tests", JSONArray().apply {
                tests.forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("testNumber", t.testNumber)
                        put("subject", t.subject)
                        put("topic", t.topic)
                        put("testDate", t.testDate)
                        put("totalMarks", t.totalMarks ?: JSONObject.NULL)
                        put("marksScored", t.marksScored ?: JSONObject.NULL)
                        put("correctCount", t.correctCount ?: JSONObject.NULL)
                        put("incorrectCount", t.incorrectCount ?: JSONObject.NULL)
                        put("skippedCount", t.skippedCount ?: JSONObject.NULL)
                        put("timeTakenMinutes", t.timeTakenMinutes ?: JSONObject.NULL)
                        put("notes", t.notes)
                    })
                }
            })
            put("revisions", JSONArray().apply {
                revisions.forEach { r ->
                    put(JSONObject().apply {
                        put("id", r.id)
                        put("testId", r.testId)
                        put("stageCode", r.stageCode)
                        put("scheduledDate", r.scheduledDate)
                        put("isCompleted", r.isCompleted)
                        put("completedDate", r.completedDate ?: JSONObject.NULL)
                        put("difficulty", r.difficulty ?: JSONObject.NULL)
                        put("notes", r.notes)
                    })
                }
            })
            put("questions", JSONArray().apply {
                questions.forEach { q ->
                    put(JSONObject().apply {
                        put("id", q.id)
                        put("testId", q.testId)
                        put("questionNumber", q.questionNumber)
                        put("errorCode", q.errorCode)
                        put("topicTag", q.topicTag)
                        put("notes", q.notes)
                        put("createdDate", q.createdDate)
                    })
                }
            })
            put("settings", JSONObject().apply {
                put("darkMode", settings.darkMode)
                put("examTitle", settings.examTitle)
                put("branchSubtitle", settings.branchSubtitle)
            })
        }
        return root.toString(2)
    }

    fun importFromJson(jsonString: String): Result<Int> {
        return try {
            val root = JSONObject(jsonString)
            val testsArray = root.optJSONArray("tests") ?: JSONArray()
            val revisionsArray = root.optJSONArray("revisions")
            val questionsArray = root.optJSONArray("questions") ?: JSONArray()

            val parsedTests = mutableListOf<TestRecord>()
            for (i in 0 until testsArray.length()) {
                val obj = testsArray.getJSONObject(i)
                val id = obj.optString("id").ifBlank { UUID.randomUUID().toString() }
                parsedTests.add(
                    TestRecord(
                        id = id,
                        testNumber = obj.optString("testNumber", obj.optString("name", "Test-${i + 1}")),
                        subject = obj.optString("subject", "General"),
                        topic = obj.optString("topic", ""),
                        testDate = obj.optString("testDate", obj.optString("date", todayIso())).take(10),
                        totalMarks = obj.optNullableDouble("totalMarks"),
                        marksScored = obj.optNullableDouble("marksScored") ?: obj.optNullableDouble("score"),
                        correctCount = obj.optNullableInt("correctCount") ?: obj.optNullableInt("correct"),
                        incorrectCount = obj.optNullableInt("incorrectCount") ?: obj.optNullableInt("incorrect"),
                        skippedCount = obj.optNullableInt("skippedCount") ?: obj.optNullableInt("skipped"),
                        timeTakenMinutes = obj.optNullableInt("timeTakenMinutes") ?: obj.optNullableInt("timeTaken"),
                        notes = obj.optString("notes", "")
                    )
                )
            }

            val parsedRevisions = mutableListOf<RevisionRecord>()
            if (revisionsArray != null && revisionsArray.length() > 0) {
                for (i in 0 until revisionsArray.length()) {
                    val obj = revisionsArray.getJSONObject(i)
                    val testId = obj.optString("testId")
                    val stage = obj.optString("stageCode", obj.optString("stage", "R1"))
                    if (testId.isNotBlank()) {
                        parsedRevisions.add(
                            RevisionRecord(
                                id = obj.optString("id").ifBlank { "${testId}_$stage" },
                                testId = testId,
                                stageCode = stage,
                                scheduledDate = obj.optString("scheduledDate", obj.optString("dueDate", todayIso())).take(10),
                                isCompleted = obj.optBoolean("isCompleted", obj.optBoolean("completed", false)),
                                completedDate = obj.optNullableString("completedDate"),
                                difficulty = obj.optNullableString("difficulty"),
                                notes = obj.optString("notes", "")
                            )
                        )
                    }
                }
            } else {
                // Auto-generate R0..R7 for imported tests if revisions array was omitted
                parsedTests.forEach { t -> parsedRevisions.addAll(generateRevisionsForTest(t)) }
            }

            val parsedQuestions = mutableListOf<QuestionRecord>()
            for (i in 0 until questionsArray.length()) {
                val obj = questionsArray.getJSONObject(i)
                val testId = obj.optString("testId")
                if (testId.isNotBlank()) {
                    parsedQuestions.add(
                        QuestionRecord(
                            id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                            testId = testId,
                            questionNumber = obj.optString("questionNumber", obj.optString("qNo", "Q1")),
                            errorCode = obj.optString("errorCode", obj.optString("errorType", "C")).uppercase().take(1),
                            topicTag = obj.optString("topicTag", obj.optString("topic", "")),
                            notes = obj.optString("notes", ""),
                            createdDate = obj.optString("createdDate", todayIso()).take(10)
                        )
                    )
                }
            }

            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                db.delete("questions", null, null)
                db.delete("revisions", null, null)
                db.delete("tests", null, null)
                parsedTests.forEach { t ->
                    db.insertWithOnConflict("tests", null, t.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
                }
                parsedRevisions.forEach { r ->
                    db.insertWithOnConflict("revisions", null, r.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
                }
                parsedQuestions.forEach { q ->
                    db.insertWithOnConflict("questions", null, q.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            Result.success(parsedTests.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportToCsv(): String {
        val tests = getAllTests()
        val revisionsByTest = getAllRevisions().groupBy { it.testId }
        val sb = StringBuilder()
        sb.appendLine("Test Number,Subject,Topic,Date,Total Marks,Marks Scored,Correct,Incorrect,Skipped,Accuracy %,Time (min),R0,R1,R2,R3,R4,R5,R6,R7,Notes")
        tests.forEach { t ->
            val revMap = revisionsByTest[t.id].orEmpty().associateBy { it.stageCode }
            val stages = SPACING_SCHEDULE.joinToString(",") { s ->
                val r = revMap[s.code]
                val cell = if (r == null) "" else if (r.isCompleted) "${r.scheduledDate} (Done)" else r.scheduledDate
                csvEscape(cell)
            }
            sb.appendLine(
                listOf(
                    csvEscape(t.testNumber),
                    csvEscape(t.subject),
                    csvEscape(t.topic),
                    csvEscape(t.testDate),
                    t.totalMarks?.toString() ?: "",
                    t.marksScored?.toString() ?: "",
                    t.correctCount?.toString() ?: "",
                    t.incorrectCount?.toString() ?: "",
                    t.skippedCount?.toString() ?: "",
                    t.accuracyPercent?.toString() ?: "",
                    t.timeTakenMinutes?.toString() ?: ""
                ).joinToString(",") + "," + stages + "," + csvEscape(t.notes)
            )
        }
        return sb.toString()
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun Cursor.toTestRecord(): TestRecord = TestRecord(
        id = getString(getColumnIndexOrThrow("id")),
        testNumber = getString(getColumnIndexOrThrow("test_number")),
        subject = getString(getColumnIndexOrThrow("subject")),
        topic = getString(getColumnIndexOrThrow("topic")),
        testDate = getString(getColumnIndexOrThrow("test_date")),
        totalMarks = getNullableDouble("total_marks"),
        marksScored = getNullableDouble("marks_scored"),
        correctCount = getNullableInt("correct_count"),
        incorrectCount = getNullableInt("incorrect_count"),
        skippedCount = getNullableInt("skipped_count"),
        timeTakenMinutes = getNullableInt("time_taken_minutes"),
        notes = getString(getColumnIndexOrThrow("notes"))
    )

    private fun Cursor.toRevisionRecord(): RevisionRecord = RevisionRecord(
        id = getString(getColumnIndexOrThrow("id")),
        testId = getString(getColumnIndexOrThrow("test_id")),
        stageCode = getString(getColumnIndexOrThrow("stage_code")),
        scheduledDate = getString(getColumnIndexOrThrow("scheduled_date")),
        isCompleted = getInt(getColumnIndexOrThrow("is_completed")) == 1,
        completedDate = getNullableString("completed_date"),
        difficulty = getNullableString("difficulty"),
        notes = getString(getColumnIndexOrThrow("notes"))
    )

    private fun Cursor.toQuestionRecord(): QuestionRecord = QuestionRecord(
        id = getString(getColumnIndexOrThrow("id")),
        testId = getString(getColumnIndexOrThrow("test_id")),
        questionNumber = getString(getColumnIndexOrThrow("question_number")),
        errorCode = getString(getColumnIndexOrThrow("error_code")),
        topicTag = getString(getColumnIndexOrThrow("topic_tag")),
        notes = getString(getColumnIndexOrThrow("notes")),
        createdDate = getString(getColumnIndexOrThrow("created_date"))
    )

    private fun Cursor.getNullableDouble(col: String): Double? {
        val idx = getColumnIndexOrThrow(col)
        return if (isNull(idx)) null else getDouble(idx)
    }

    private fun Cursor.getNullableInt(col: String): Int? {
        val idx = getColumnIndexOrThrow(col)
        return if (isNull(idx)) null else getInt(idx)
    }

    private fun Cursor.getNullableString(col: String): String? {
        val idx = getColumnIndexOrThrow(col)
        return if (isNull(idx)) null else getString(idx)
    }

    private fun TestRecord.toContentValues() = ContentValues().apply {
        put("id", id)
        put("test_number", testNumber)
        put("subject", subject)
        put("topic", topic)
        put("test_date", testDate)
        if (totalMarks != null) put("total_marks", totalMarks) else putNull("total_marks")
        if (marksScored != null) put("marks_scored", marksScored) else putNull("marks_scored")
        if (correctCount != null) put("correct_count", correctCount) else putNull("correct_count")
        if (incorrectCount != null) put("incorrect_count", incorrectCount) else putNull("incorrect_count")
        if (skippedCount != null) put("skipped_count", skippedCount) else putNull("skipped_count")
        if (timeTakenMinutes != null) put("time_taken_minutes", timeTakenMinutes) else putNull("time_taken_minutes")
        put("notes", notes)
    }

    private fun RevisionRecord.toContentValues() = ContentValues().apply {
        put("id", id)
        put("test_id", testId)
        put("stage_code", stageCode)
        put("scheduled_date", scheduledDate)
        put("is_completed", if (isCompleted) 1 else 0)
        if (completedDate != null) put("completed_date", completedDate) else putNull("completed_date")
        if (difficulty != null) put("difficulty", difficulty) else putNull("difficulty")
        put("notes", notes)
    }

    private fun QuestionRecord.toContentValues() = ContentValues().apply {
        put("id", id)
        put("test_id", testId)
        put("question_number", questionNumber)
        put("error_code", errorCode)
        put("topic_tag", topicTag)
        put("notes", notes)
        put("created_date", createdDate)
    }

    private fun JSONObject.optNullableDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val v = optDouble(key, Double.NaN)
        return if (v.isNaN()) null else v
    }

    private fun JSONObject.optNullableInt(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        val raw = opt(key) ?: return null
        return when (raw) {
            is Number -> raw.toInt()
            is String -> raw.trim().toIntOrNull()
            else -> null
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() }
    }
}
