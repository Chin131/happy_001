package com.example.notepro.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notepro.data.model.Note
import com.example.notepro.data.repository.NoteRepository
import com.example.notepro.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    // 搜索查询
    private val _searchQuery = MutableStateFlow("")
    
    // 过滤器状态
    private val _titleFilter = MutableStateFlow(true)
    private val _contentFilter = MutableStateFlow(true)
    private val _tagsFilter = MutableStateFlow(false)
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    // 搜索结果
    val searchResults = combine(
        _searchQuery,
        _titleFilter,
        _contentFilter,
        _tagsFilter
    ) { query, titleFilter, contentFilter, tagsFilter ->
        // 返回过滤器和查询组合的四元组
        FilterCriteria(query, titleFilter, contentFilter, tagsFilter)
    }
    .flatMapLatest { criteria ->
        // 当查询为空且没有过滤器变化时，返回空列表
        if (criteria.query.isBlank()) {
            return@flatMapLatest flowOf(emptyList())
        }
        
        _isLoading.value = true
        
        // 执行搜索
        val results = noteRepository.searchNotes(
            query = criteria.query,
            searchInTitle = criteria.titleFilter,
            searchInContent = criteria.contentFilter,
            searchInTags = criteria.tagsFilter
        )
        
        _isLoading.value = false
        results
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )
    
    // 设置搜索查询
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    // 设置标题过滤器
    fun setTitleFilter(enabled: Boolean) {
        _titleFilter.value = enabled
    }
    
    // 设置内容过滤器
    fun setContentFilter(enabled: Boolean) {
        _contentFilter.value = enabled
    }
    
    // 设置标签过滤器
    fun setTagsFilter(enabled: Boolean) {
        _tagsFilter.value = enabled
    }
    
    // 检查是否有活动的搜索查询
    fun hasActiveQuery(): Boolean {
        return _searchQuery.value.isNotBlank()
    }
    
    // 过滤条件数据类
    data class FilterCriteria(
        val query: String,
        val titleFilter: Boolean,
        val contentFilter: Boolean,
        val tagsFilter: Boolean
    )
} 