package com.example.notepro.ui.screen.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notepro.R
import com.example.notepro.domain.model.Note
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesAdapter(
    private val onNoteClick: (Note) -> Unit,
    private val onNoteLongClick: (Note) -> Unit
) : ListAdapter<Note, NotesAdapter.NoteViewHolder>(NoteDiffCallback()) {

    private var viewMode = ViewMode.LIST
    private var onReorderListener: ((List<Note>) -> Unit)? = null

    fun setViewMode(mode: ViewMode) {
        if (viewMode != mode) {
            viewMode = mode
            notifyItemRangeChanged(0, itemCount)
        }
    }

    fun getNoteAt(position: Int): Note = getItem(position)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        holder.bind(note, viewMode, onNoteClick, onNoteLongClick)
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val currentList = currentList.toMutableList()
        val item = currentList.removeAt(fromPosition)
        currentList.add(toPosition, item)
        
        // 提交新的列表
        submitList(currentList)
    }

    fun setOnReorderListener(listener: (List<Note>) -> Unit) {
        onReorderListener = listener
    }

    fun onReorderFinished() {
        onReorderListener?.invoke(currentList)
    }

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.note_card)
        private val titleTextView: TextView = itemView.findViewById(R.id.note_title)
        private val contentTextView: TextView = itemView.findViewById(R.id.note_content)
        private val dateTextView: TextView = itemView.findViewById(R.id.note_date)
        private val pinIcon: ImageView = itemView.findViewById(R.id.pin_icon)
        private val tagChip: Chip = itemView.findViewById(R.id.note_tag)

        fun bind(
            note: Note,
            viewMode: ViewMode,
            onNoteClick: (Note) -> Unit,
            onNoteLongClick: (Note) -> Unit
        ) {
            // Set card width based on view mode
            val params = cardView.layoutParams
            if (viewMode == ViewMode.GRID) {
                // Grid layout adjustments if needed
                contentTextView.maxLines = 4
            } else {
                // List layout adjustments if needed
                contentTextView.maxLines = 6
            }
            cardView.layoutParams = params

            // Set card color if available
            note.color?.let { color ->
                cardView.setCardBackgroundColor(color)
            }

            // Set title
            if (note.title.isEmpty()) {
                titleTextView.visibility = View.GONE
            } else {
                titleTextView.visibility = View.VISIBLE
                titleTextView.text = note.title
            }

            // Set content
            contentTextView.text = note.content

            // Set date
            dateTextView.text = formatDateTime(note.modifiedAt)

            // Set pin icon visibility
            pinIcon.isVisible = note.isPinned

            // Set tag if available
            if (note.tags.isNotEmpty()) {
                tagChip.visibility = View.VISIBLE
                tagChip.text = note.tags.first().name
            } else {
                tagChip.visibility = View.GONE
            }

            // Set click listeners
            cardView.setOnClickListener { onNoteClick(note) }
            cardView.setOnLongClickListener {
                onNoteLongClick(note)
                true
            }
        }

        private fun formatDateTime(timestamp: Long): String {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return dateFormat.format(Date(timestamp))
        }
    }

    private class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
} 