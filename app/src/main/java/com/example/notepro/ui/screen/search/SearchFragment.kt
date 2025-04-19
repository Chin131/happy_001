package com.example.notepro.ui.screen.search

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notepro.data.model.Note
import com.example.notepro.databinding.FragmentSearchBinding
import com.example.notepro.ui.component.NoteItemAnimator
import com.example.notepro.ui.screen.home.NotesAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var searchAdapter: NotesAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupSearchInput()
        setupFilterChips()
        setupRecyclerView()
        observeViewModel()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            hideKeyboard()
            findNavController().navigateUp()
        }
    }
    
    private fun setupSearchInput() {
        binding.searchEditText.apply {
            // 设置文本变化监听
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                
                override fun afterTextChanged(s: Editable?) {
                    viewModel.setSearchQuery(s.toString())
                }
            })
            
            // 设置键盘搜索按钮监听
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // 隐藏键盘
                    hideKeyboard()
                    return@setOnEditorActionListener true
                }
                false
            }
            
            // 获取焦点并显示键盘
            requestFocus()
            showKeyboard()
        }
    }
    
    private fun setupFilterChips() {
        // 标题过滤器
        binding.filterTitle.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setTitleFilter(isChecked)
        }
        
        // 内容过滤器
        binding.filterContent.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setContentFilter(isChecked)
        }
        
        // 标签过滤器
        binding.filterTags.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setTagsFilter(isChecked)
        }
    }
    
    private fun setupRecyclerView() {
        searchAdapter = NotesAdapter(
            onNoteClick = { note ->
                findNavController().navigate(
                    SearchFragmentDirections.actionSearchFragmentToEditorFragment(noteId = note.id)
                )
            },
            onNoteLongClick = { note ->
                showNoteOptionsBottomSheet(note)
            }
        )
        
        binding.searchResultsRecyclerView.apply {
            adapter = searchAdapter
            layoutManager = LinearLayoutManager(requireContext())
            itemAnimator = NoteItemAnimator()
        }
    }
    
    private fun observeViewModel() {
        // 观察搜索结果
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.searchResults.collect { results ->
                searchAdapter.submitList(results)
                updateEmptyState(results.isEmpty())
            }
        }
        
        // 观察加载状态
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.loadingAnimation.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptySearchView.visibility = if (isEmpty && viewModel.hasActiveQuery()) View.VISIBLE else View.GONE
        binding.searchResultsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    private fun showNoteOptionsBottomSheet(note: Note) {
        val bottomSheet = NoteOptionsBottomSheet.newInstance(note.id)
        bottomSheet.show(childFragmentManager, "NoteOptionsBottomSheet")
    }
    
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
    }
    
    private fun showKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.searchEditText, InputMethodManager.SHOW_IMPLICIT)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 