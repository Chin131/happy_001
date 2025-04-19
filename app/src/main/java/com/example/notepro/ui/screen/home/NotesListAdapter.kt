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

class NotesListAdapter(
    private val onNoteClick: (Note, View) -> Unit,
    private val onNoteLongClick: (Note) -> Unit
) : ListAdapter<Note, RecyclerView.ViewHolder>(NoteDiffCallback()) {

    companion object {
        const val VIEW_TYPE_LIST = 0
        const val VIEW_TYPE_GRID = 1
    }

    private var viewType = VIEW_TYPE_LIST

    fun setViewType(viewType: Int) {
        if (this.viewType != viewType) {
            this.viewType = viewType
            notifyDataSetChanged()
        }
    }

    fun getNoteAt(position: Int): Note = getItem(position)

    override fun getItemViewType(position: Int): Int {
        return viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_LIST -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_note_list, parent, false)
                ListViewHolder(view)
            }
            VIEW_TYPE_GRID -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_note_card, parent, false)
                GridViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val note = getItem(position)
        when (holder) {
            is ListViewHolder -> holder.bind(note)
            is GridViewHolder -> holder.bind(note)
        }
    }

    inner class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.note_card)
        private val titleTextView: TextView = itemView.findViewById(R.id.note_title)
        private val contentTextView: TextView = itemView.findViewById(R.id.note_content)
        private val dateTextView: TextView = itemView.findViewById(R.id.note_date)
        private val pinIcon: ImageView = itemView.findViewById(R.id.pin_icon)
        private val tagChip: Chip = itemView.findViewById(R.id.note_tag)

        init {
            cardView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onNoteClick(getItem(position), cardView)
                }
            }

            cardView.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onNoteLongClick(getItem(position))
                    true
                } else {
                    false
                }
            }
        }

        fun bind(note: Note) {
            // Set up transition names for shared element transitions
            cardView.transitionName = "note_card_${note.id}"
            titleTextView.transitionName = "note_title_${note.id}"
            
            // Set card color if available
            note.color?.let { color ->
                cardView.setCardBackgroundColor(color)
            }

            // Set title
            if (note.title.isEmpty()) {
                titleTextView.text = itemView.context.getString(R.string.untitled_note)
            } else {
                titleTextView.text = note.title
            }

            // Set content
            contentTextView.text = note.content
            contentTextView.isVisible = note.content.isNotEmpty()

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
        }
    }

    inner class GridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.note_card)
        private val titleTextView: TextView = itemView.findViewById(R.id.note_title)
        private val contentTextView: TextView = itemView.findViewById(R.id.note_content)
        private val dateTextView: TextView = itemView.findViewById(R.id.note_date)
        private val pinIcon: ImageView = itemView.findViewById(R.id.pin_icon)
        private val tagChip: Chip = itemView.findViewById(R.id.note_tag)

        init {
            cardView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onNoteClick(getItem(position), cardView)
                }
            }

            cardView.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onNoteLongClick(getItem(position))
                    true
                } else {
                    false
                }
            }
        }

        fun bind(note: Note) {
            // Set up transition names for shared element transitions
            cardView.transitionName = "note_card_${note.id}"
            titleTextView.transitionName = "note_title_${note.id}"
            
            // Set card color if available
            note.color?.let { color ->
                cardView.setCardBackgroundColor(color)
            }

            // Set title
            if (note.title.isEmpty()) {
                titleTextView.text = itemView.context.getString(R.string.untitled_note)
            } else {
                titleTextView.text = note.title
            }

            // Set content
            contentTextView.text = note.content
            contentTextView.isVisible = note.content.isNotEmpty()

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
        }
    }

    private fun formatDateTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
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