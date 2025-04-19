package com.example.notepro.ui.screen.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.notepro.R
import com.example.notepro.databinding.FragmentHomeBinding
import com.example.notepro.domain.model.Note
import com.example.notepro.ui.component.common.SwipeToDeleteCallback
import com.example.notepro.util.animation.NoteItemAnimator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var notesAdapter: NotesListAdapter
    private lateinit var recyclerView: RecyclerView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Find views directly
        recyclerView = view.findViewById(R.id.notes_recycler_view)
        
        setupToolbar()
        setupRecyclerView()
        setupSwipeRefresh()
        setupFab()
        setupCategoryTabs()
        observeViewModel()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_search -> {
                    // Navigate to search
                    // findNavController().navigate(R.id.action_homeFragment_to_searchFragment)
                    true
                }
                R.id.action_view_mode -> {
                    toggleViewMode()
                    true
                }
                R.id.action_settings -> {
                    // Navigate to settings
                    // findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupRecyclerView() {
        notesAdapter = NotesListAdapter(
            onNoteClick = { note, sharedElement ->
                // Create shared element transition
                val extras = FragmentNavigatorExtras(
                    sharedElement to "note_card_transition"
                )
                
                // Navigate to editor with note ID using shared element transition
                // val action = HomeFragmentDirections.actionHomeFragmentToEditorFragment(noteId = note.id)
                // findNavController().navigate(action, extras)
            },
            onNoteLongClick = { note ->
                // Show note options
                showNoteOptionsBottomSheet(note)
            }
        )
        
        recyclerView.apply {
            adapter = notesAdapter
            layoutManager = viewModel.getLayoutManager(requireContext())
            itemAnimator = NoteItemAnimator()
            
            // Add swipe-to-delete functionality
            val swipeHandler = object : SwipeToDeleteCallback(requireContext()) {
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.bindingAdapterPosition
                    val note = notesAdapter.getNoteAt(position)
                    viewModel.moveNoteToTrash(note)
                    
                    Snackbar.make(
                        binding.root,
                        R.string.note_moved_to_trash,
                        Snackbar.LENGTH_LONG
                    ).setAction(R.string.undo) {
                        viewModel.restoreNoteFromTrash(note)
                    }.show()
                }
            }
            
            ItemTouchHelper(swipeHandler).attachToRecyclerView(this)
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshNotes()
        }
    }
    
    private fun setupFab() {
        binding.fabAddNote.setOnClickListener {
            // Navigate to editor to create a new note
            // findNavController().navigate(HomeFragmentDirections.actionHomeFragmentToEditorFragment())
        }
    }
    
    private fun setupCategoryTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val categoryId = tab.tag as? String
                viewModel.setSelectedCategory(categoryId)
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
    
    private fun observeViewModel() {
        // Observe notes
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notes.collectLatest { notes ->
                notesAdapter.submitList(notes)
                updateEmptyState(notes.isEmpty())
                binding.swipeRefresh.isRefreshing = false
            }
        }
        
        // Observe categories
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categories.collectLatest { categories ->
                updateCategoryTabs(categories)
            }
        }
        
        // Observe view mode
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.viewMode.collectLatest { viewMode ->
                updateViewModeIcon(viewMode)
                recyclerView.layoutManager = viewModel.getLayoutManager(requireContext())
                notesAdapter.setViewType(
                    if (viewMode == ViewMode.LIST) 
                        NotesListAdapter.VIEW_TYPE_LIST 
                    else 
                        NotesListAdapter.VIEW_TYPE_GRID
                )
            }
        }
        
        // Observe refreshing state
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isRefreshing.collectLatest { isRefreshing ->
                binding.swipeRefresh.isRefreshing = isRefreshing
            }
        }
    }
    
    private fun updateCategoryTabs(categories: List<com.example.notepro.domain.model.Category>) {
        binding.tabLayout.removeAllTabs()
        
        // Add "All Notes" tab
        binding.tabLayout.addTab(
            binding.tabLayout.newTab()
                .setText(R.string.all_notes)
                .setTag(null)
        )
        
        // Add category tabs
        categories.forEach { category ->
            binding.tabLayout.addTab(
                binding.tabLayout.newTab()
                    .setText(category.name)
                    .setTag(category.id.toString())
                    // You can set an icon for the category if available
                    // .setIcon(R.drawable.ic_folder)
            )
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyView.root.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
    
    private fun toggleViewMode() {
        viewModel.toggleViewMode()
    }
    
    private fun updateViewModeIcon(viewMode: ViewMode) {
        val menuItem = binding.toolbar.menu.findItem(R.id.action_view_mode)
        menuItem?.setIcon(
            if (viewMode == ViewMode.GRID) 
                R.drawable.ic_view_list 
            else 
                R.drawable.ic_view_grid
        )
    }
    
    private fun showNoteOptionsBottomSheet(note: Note) {
        // Here you would show a bottom sheet with options like Pin, Archive, Delete, etc.
        // For now, we'll just show a snackbar as a placeholder
        Snackbar.make(
            binding.root,
            "Note options for: ${note.title}",
            Snackbar.LENGTH_SHORT
        ).show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 