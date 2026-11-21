package com.testtracker.gate.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE tests (
                id TEXT PRIMARY KEY,
                test_number TEXT NOT NULL,
                subject TEXT NOT NULL,
                topic TEXT NOT NULL,
                test_date TEXT NOT NULL,
                total_marks REAL,
                marks_scored REAL,
                correct_count INTEGER,
                incorrect_count INTEGER,
                skipped_count INTEGER,
                time_taken_minutes INTEGER,
                notes TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE revisions (
                id TEXT PRIMARY KEY,
                test_id TEXT NOT NULL,
                stage_code TEXT NOT NULL,
                scheduled_date TEXT NOT NULL,
                is_completed INTEGER NOT NULL DEFAULT 0,
                completed_date TEXT,
                difficulty TEXT,
                notes TEXT NOT NULL DEFAULT '',
                FOREIGN KEY(test_id) REFERENCES tests(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE questions (
                id TEXT PRIMARY KEY,
                test_id TEXT NOT NULL,
                question_number TEXT NOT NULL,
                error_code TEXT NOT NULL,
                topic_tag TEXT NOT NULL DEFAULT '',
                notes TEXT NOT NULL DEFAULT '',
                created_date TEXT NOT NULL,
                FOREIGN KEY(test_id) REFERENCES tests(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE settings (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX idx_revisions_test_id ON revisions(test_id)")
        db.execSQL("CREATE INDEX idx_revisions_date ON revisions(scheduled_date)")
        db.execSQL("CREATE INDEX idx_questions_test_id ON questions(test_id)")
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS questions")
        db.execSQL("DROP TABLE IF EXISTS revisions")
        db.execSQL("DROP TABLE IF EXISTS tests")
        db.execSQL("DROP TABLE IF EXISTS settings")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "test_tracker_gate.db"
        private const val DATABASE_VERSION = 1
    }
}
