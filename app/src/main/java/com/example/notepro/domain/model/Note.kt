package com.example.notepro.domain.model

import java.util.Date

/**
 * Domain model for a Note
 */
data class Note(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val contentType: ContentType = ContentType.TEXT,
    val createdAt: Long = Date().time,
    val modifiedAt: Long = Date().time,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val color: Int? = null,
    val reminderTime: Long? = null,
    val categoryId: Long? = null,
    val tags: List<Tag> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus.NOT_SYNCED
)

enum class ContentType {
    TEXT,
    RICH_TEXT,
    MARKDOWN,
    CHECKLIST,
    VOICE,
    DRAWING
}

enum class SyncStatus {
    SYNCED,
    NOT_SYNCED,
    SYNCING,
    SYNC_FAILED
} 