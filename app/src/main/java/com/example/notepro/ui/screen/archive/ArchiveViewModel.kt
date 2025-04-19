package com.example.notepro.ui.screen.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notepro.domain.model.Note
import com.example.notepro.domain.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    val archivedNotes: Flow<List<Note>> = noteRepository.getArchivedNotes()

    fun refreshNotes() {
        viewModelScope.launch {
            noteRepository.refreshNotes()
        }
    }

    fun unarchiveNote(note: Note) {
        viewModelScope.launch {
            val updatedNote = note.copy(isArchived = false)
            noteRepository.updateNote(updatedNote)
        }
    }

    fun moveNoteToTrash(note: Note) {
        viewModelScope.launch {
            val updatedNote = note.copy(isInTrash = true)
            noteRepository.updateNote(updatedNote)
        }
    }

    fun restoreNoteFromTrash(note: Note) {
        viewModelScope.launch {
            val updatedNote = note.copy(isInTrash = false)
            noteRepository.updateNote(updatedNote)
        }
    }
} 