package com.example.notepro.ui.screen.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notepro.domain.model.Category
import com.example.notepro.domain.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ViewMode {
    LIST, GRID
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    // These will be injected by Hilt once we implement the repositories
    // private val noteRepository: NoteRepository,
    // private val categoryRepository: CategoryRepository,
    // private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    // Current view mode (list or grid)
    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    // Currently selected category
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    // For handling loading states
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Mock data until repositories are implemented
    private val _categories = MutableStateFlow<List<Category>>(
        listOf(
            Category(id = 1, name = "Work"),
            Category(id = 2, name = "Personal"),
            Category(id = 3, name = "Ideas")
        )
    )
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _allNotes = MutableStateFlow<List<Note>>(
        listOf(
            Note(
                id = 1,
                title = "Meeting Notes",
                content = "Discuss project timeline and deliverables.",
                isPinned = true,
                categoryId = 1
            ),
            Note(
                id = 2,
                title = "Shopping List",
                content = "- Milk\n- Eggs\n- Bread\n- Apples",
                categoryId = 2
            ),
            Note(
                id = 3,
                title = "App Ideas",
                content = "1. Food delivery app\n2. Fitness tracker\n3. Language learning app",
                categoryId = 3
            )
        )
    )

    // Notes filtered by selected category
    val notes = _selectedCategoryId.flatMapLatest { categoryId ->
        MutableStateFlow(
            if (categoryId == null) {
                _allNotes.value
            } else {
                _allNotes.value.filter { it.categoryId.toString() == categoryId }
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = _allNotes.value
    )

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
        // In a real app, we would save this preference
        // viewModelScope.launch {
        //     preferencesRepository.saveViewMode(_viewMode.value)
        // }
    }

    fun setSelectedCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun refreshNotes() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // In a real app, we would refresh data from the repository
            // noteRepository.refreshNotes()
            _isRefreshing.value = false
        }
    }

    fun moveNoteToTrash(note: Note) {
        // In a real app, we would update the repository
        // viewModelScope.launch {
        //     noteRepository.moveToTrash(note.id)
        // }
        
        // For now, just update our mock data
        _allNotes.value = _allNotes.value.map {
            if (it.id == note.id) it.copy(isDeleted = true) else it
        }
    }

    fun restoreNoteFromTrash(note: Note) {
        // In a real app, we would update the repository
        // viewModelScope.launch {
        //     noteRepository.restoreFromTrash(note.id)
        // }
        
        // For now, just update our mock data
        _allNotes.value = _allNotes.value.map {
            if (it.id == note.id) it.copy(isDeleted = false) else it
        }
    }

    fun getLayoutManager(context: Context): RecyclerView.LayoutManager {
        return if (_viewMode.value == ViewMode.GRID) {
            GridLayoutManager(context, 2)
        } else {
            LinearLayoutManager(context)
        }
    }
} 