package com.example.notepro.ui.screen.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notepro.R
import com.example.notepro.databinding.ItemNoteCardBinding
import com.example.notepro.databinding.ItemNoteListBinding
import com.example.notepro.model.Note
import java.text.SimpleDateFormat
import java.util.Locale

class NoteAdapter(
    private val context: Context,
    private val onNoteClick: (Note) -> Unit,
    private val onNoteLongClick: (Note) -> Boolean
) : ListAdapter<Note, RecyclerView.ViewHolder>(NoteDiffCallback()) {

    companion object {
        const val VIEW_TYPE_LIST = 0
        const val VIEW_TYPE_GRID = 1
    }

    private var viewType = VIEW_TYPE_LIST

    fun setViewType(viewType: Int) {
        this.viewType = viewType
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_LIST -> {
                val binding = ItemNoteListBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ListViewHolder(binding)
            }
            VIEW_TYPE_GRID -> {
                val binding = ItemNoteCardBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                GridViewHolder(binding)
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

    override fun getItemViewType(position: Int): Int {
        return viewType
    }

    inner class ListViewHolder(private val binding: ItemNoteListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onNoteClick(getItem(position))
                }
            }

            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onNoteLongClick(getItem(position))
                } else {
                    false
                }
            }
        }

        fun bind(note: Note) {
            binding.apply {
                textTitle.text = note.title.ifEmpty { context.getString(R.string.untitled_note) }
                textContent.text = note.content
                textContent.visibility = if (note.content.isNotEmpty()) View.VISIBLE else View.GONE
                
                // Format date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                textDate.text = dateFormat.format(note.updatedAt)
                
                // Show pinned indicator
                pinIndicator.visibility = if (note.isPinned) View.VISIBLE else View.GONE
            }
        }
    }

    inner class GridViewHolder(private val binding: ItemNoteCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onNoteClick(getItem(position))
                }
            }

            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onNoteLongClick(getItem(position))
                } else {
                    false
                }
            }
        }

        fun bind(note: Note) {
            binding.apply {
                textTitle.text = note.title.ifEmpty { context.getString(R.string.untitled_note) }
                textContent.text = note.content
                textContent.visibility = if (note.content.isNotEmpty()) View.VISIBLE else View.GONE
                
                // Format date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                textDate.text = dateFormat.format(note.updatedAt)
                
                // Show pinned indicator
                pinIndicator.visibility = if (note.isPinned) View.VISIBLE else View.GONE
            }
        }
    }

    class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
} 