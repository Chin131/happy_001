package com.example.notepro.ui.component.search

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notepro.R
import com.example.notepro.data.model.Note
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

class SearchResultAdapter(
    private val onNoteClick: (Note) -> Unit,
    private val onNoteLongClick: (Note) -> Unit
) : ListAdapter<NoteWithHighlights, SearchResultViewHolder>(SearchResultDiffCallback()) {

    private var searchQuery: String = ""
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return SearchResultViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        val noteWithHighlights = getItem(position)
        holder.bind(noteWithHighlights, searchQuery, onNoteClick, onNoteLongClick)
    }
    
    fun setSearchQuery(query: String) {
        this.searchQuery = query
        notifyDataSetChanged() // 需要重新绑定所有项以应用新的高亮
    }
}

class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val noteCard: MaterialCardView = itemView.findViewById(R.id.note_card)
    private val titleText: TextView = itemView.findViewById(R.id.note_title)
    private val contentText: TextView = itemView.findViewById(R.id.note_content)
    private val dateText: TextView = itemView.findViewById(R.id.note_date)
    
    fun bind(
        noteWithHighlights: NoteWithHighlights,
        searchQuery: String,
        onNoteClick: (Note) -> Unit,
        onNoteLongClick: (Note) -> Unit
    ) {
        val note = noteWithHighlights.note
        
        // 设置卡片背景颜色
        note.color?.let { color ->
            noteCard.setCardBackgroundColor(color)
        }
        
        // 设置标题（带高亮）
        titleText.text = if (noteWithHighlights.highlightedTitle != null) {
            noteWithHighlights.highlightedTitle
        } else {
            highlightText(note.title, searchQuery)
        }
        
        // 设置内容（带高亮）
        contentText.text = if (noteWithHighlights.highlightedContent != null) {
            noteWithHighlights.highlightedContent
        } else {
            highlightText(note.content, searchQuery)
        }
        
        // 设置日期
        dateText.text = formatDateTime(note.modifiedAt)
        
        // 设置点击事件
        noteCard.setOnClickListener { onNoteClick(note) }
        noteCard.setOnLongClickListener { 
            onNoteLongClick(note)
            true
        }
    }
    
    private fun highlightText(text: String, query: String): SpannableString {
        if (query.isBlank()) return SpannableString(text)
        
        val spannableString = SpannableString(text)
        val pattern = Pattern.compile(query, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        
        while (matcher.find()) {
            spannableString.setSpan(
                BackgroundColorSpan(Color.YELLOW),
                matcher.start(),
                matcher.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                ForegroundColorSpan(Color.BLACK),
                matcher.start(),
                matcher.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                matcher.start(),
                matcher.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        
        return spannableString
    }
    
    private fun formatDateTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}

// 搜索结果差异比较
class SearchResultDiffCallback : DiffUtil.ItemCallback<NoteWithHighlights>() {
    override fun areItemsTheSame(oldItem: NoteWithHighlights, newItem: NoteWithHighlights): Boolean {
        return oldItem.note.id == newItem.note.id
    }
    
    override fun areContentsTheSame(oldItem: NoteWithHighlights, newItem: NoteWithHighlights): Boolean {
        return oldItem == newItem
    }
}

// 带高亮文本的笔记数据类
data class NoteWithHighlights(
    val note: Note,
    val highlightedTitle: SpannableString? = null,
    val highlightedContent: SpannableString? = null
) 