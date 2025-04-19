package com.example.notepro.ui.screen.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notepro.domain.model.Note
import com.example.notepro.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    val trashedNotes: StateFlow<List<Note>> = noteRepository.getTrashedNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun refreshNotes() {
        viewModelScope.launch {
            noteRepository.refreshNotes()
        }
    }

    fun deleteNotePermanently(note: Note) {
        viewModelScope.launch {
            noteRepository.deleteNotePermanently(note.id)
        }
    }

    fun restoreNote(note: Note) {
        viewModelScope.launch {
            noteRepository.restoreNoteFromTrash(note.id)
        }
    }

    fun moveNoteToTrash(note: Note) {
        viewModelScope.launch {
            noteRepository.moveNoteToTrash(note.id)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            noteRepository.emptyTrash()
        }
    }
} 