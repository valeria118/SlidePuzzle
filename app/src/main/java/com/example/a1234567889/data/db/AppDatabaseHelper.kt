package com.example.a1234567889.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Lab item 15 — "Создание БД".
 *
 * A genuine relational SQLite database created with [SQLiteOpenHelper] (CREATE TABLE,
 * INSERT, SELECT, DELETE — raw SQL, no ORM) used for two purposes elsewhere in the app:
 *  - [TABLE_GAMES]    — history of finished matches (shown in GameHistoryScreen, item 15).
 *  - [TABLE_MESSAGES] — persisted chat messages for the classic ChatActivity (items 11/12/18).
 */
class AppDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "puzzle_app.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_GAMES = "game_history"
        const val TABLE_MESSAGES = "chat_messages"

        // game_history columns
        const val COL_ID = "id"
        const val COL_MODE = "mode"
        const val COL_SIZE = "size"
        const val COL_MOVES = "moves"
        const val COL_TIME_SECONDS = "time_seconds"
        const val COL_WON = "won"
        const val COL_DATE = "date_millis"

        // chat_messages columns
        const val COL_MSG_ID = "id"
        const val COL_MSG_SENDER = "sender"
        const val COL_MSG_TEXT = "text"
        const val COL_MSG_COLOR = "color"
        const val COL_MSG_BOLD = "is_bold"
        const val COL_MSG_DATE = "date_millis"
        const val COL_MSG_IS_ME = "is_me"
        const val COL_MSG_VIDEO_URI = "video_uri"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_GAMES (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_MODE TEXT NOT NULL,
                $COL_SIZE INTEGER NOT NULL,
                $COL_MOVES INTEGER NOT NULL,
                $COL_TIME_SECONDS INTEGER NOT NULL,
                $COL_WON INTEGER NOT NULL,
                $COL_DATE INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_MESSAGES (
                $COL_MSG_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_MSG_SENDER TEXT NOT NULL,
                $COL_MSG_TEXT TEXT NOT NULL,
                $COL_MSG_COLOR INTEGER NOT NULL,
                $COL_MSG_BOLD INTEGER NOT NULL,
                $COL_MSG_DATE INTEGER NOT NULL,
                $COL_MSG_IS_ME INTEGER NOT NULL,
                $COL_MSG_VIDEO_URI TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GAMES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        onCreate(db)
    }

    // ---------------- game_history CRUD ----------------

    data class GameRecord(
        val id: Long,
        val mode: String,
        val size: Int,
        val moves: Int,
        val timeSeconds: Long,
        val won: Boolean,
        val dateMillis: Long
    )

    fun insertGameRecord(mode: String, size: Int, moves: Int, timeSeconds: Long, won: Boolean): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_MODE, mode)
            put(COL_SIZE, size)
            put(COL_MOVES, moves)
            put(COL_TIME_SECONDS, timeSeconds)
            put(COL_WON, if (won) 1 else 0)
            put(COL_DATE, System.currentTimeMillis())
        }
        return db.insert(TABLE_GAMES, null, values)
    }

    fun getAllGames(limit: Int = 200): List<GameRecord> {
        val db = readableDatabase
        val results = mutableListOf<GameRecord>()
        val cursor = db.query(
            TABLE_GAMES, null, null, null, null, null,
            "$COL_DATE DESC", limit.toString()
        )
        cursor.use {
            while (it.moveToNext()) {
                results.add(
                    GameRecord(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                        mode = it.getString(it.getColumnIndexOrThrow(COL_MODE)),
                        size = it.getInt(it.getColumnIndexOrThrow(COL_SIZE)),
                        moves = it.getInt(it.getColumnIndexOrThrow(COL_MOVES)),
                        timeSeconds = it.getLong(it.getColumnIndexOrThrow(COL_TIME_SECONDS)),
                        won = it.getInt(it.getColumnIndexOrThrow(COL_WON)) == 1,
                        dateMillis = it.getLong(it.getColumnIndexOrThrow(COL_DATE))
                    )
                )
            }
        }
        return results
    }

    fun clearGameHistory() {
        writableDatabase.delete(TABLE_GAMES, null, null)
    }

    fun deleteGameRecord(id: Long) {
        writableDatabase.delete(TABLE_GAMES, "$COL_ID = ?", arrayOf(id.toString()))
    }

    // ---------------- chat_messages CRUD ----------------

    data class ChatMessageRecord(
        val id: Long,
        val sender: String,
        val text: String,
        val colorArgb: Int,
        val isBold: Boolean,
        val dateMillis: Long,
        val isMe: Boolean,
        val videoUri: String?
    )

    fun insertMessage(
        sender: String,
        text: String,
        colorArgb: Int,
        isBold: Boolean,
        isMe: Boolean,
        videoUri: String? = null
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_MSG_SENDER, sender)
            put(COL_MSG_TEXT, text)
            put(COL_MSG_COLOR, colorArgb)
            put(COL_MSG_BOLD, if (isBold) 1 else 0)
            put(COL_MSG_DATE, System.currentTimeMillis())
            put(COL_MSG_IS_ME, if (isMe) 1 else 0)
            put(COL_MSG_VIDEO_URI, videoUri)
        }
        return db.insert(TABLE_MESSAGES, null, values)
    }

    fun getAllMessages(): List<ChatMessageRecord> {
        val db = readableDatabase
        val results = mutableListOf<ChatMessageRecord>()
        val cursor = db.query(TABLE_MESSAGES, null, null, null, null, null, "$COL_MSG_DATE ASC")
        cursor.use {
            while (it.moveToNext()) {
                results.add(
                    ChatMessageRecord(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_MSG_ID)),
                        sender = it.getString(it.getColumnIndexOrThrow(COL_MSG_SENDER)),
                        text = it.getString(it.getColumnIndexOrThrow(COL_MSG_TEXT)),
                        colorArgb = it.getInt(it.getColumnIndexOrThrow(COL_MSG_COLOR)),
                        isBold = it.getInt(it.getColumnIndexOrThrow(COL_MSG_BOLD)) == 1,
                        dateMillis = it.getLong(it.getColumnIndexOrThrow(COL_MSG_DATE)),
                        isMe = it.getInt(it.getColumnIndexOrThrow(COL_MSG_IS_ME)) == 1,
                        videoUri = it.getString(it.getColumnIndexOrThrow(COL_MSG_VIDEO_URI))
                    )
                )
            }
        }
        return results
    }

    fun clearMessages() {
        writableDatabase.delete(TABLE_MESSAGES, null, null)
    }
}
