package com.example.notepro.ui.screen.archive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notepro.R
import com.example.notepro.databinding.FragmentArchiveBinding
import com.example.notepro.domain.model.Note
import com.example.notepro.ui.screen.home.NotesAdapter
import com.example.notepro.util.gesture.SwipeActionCallback
import com.example.notepro.util.gone
import com.example.notepro.util.visible
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ArchiveFragment : Fragment(), NotesAdapter.OnNoteClickListener {

    private var _binding: FragmentArchiveBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ArchiveViewModel by viewModels()
    private lateinit var notesAdapter: NotesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArchiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupRecyclerView()
        setupSwipeActions()
        setupSwipeRefresh()
        observeNotes()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val menuProvider = object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // No menu items for archive screen
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return false
            }
        }

        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter().apply {
            setOnNoteClickListener(this@ArchiveFragment)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notesAdapter
        }
    }

    private fun setupSwipeActions() {
        val swipeCallback = SwipeActionCallback(
            requireContext(),
            R.drawable.ic_unarchive,
            R.color.archive_background,
            ItemTouchHelper.LEFT
        ) { position ->
            val note = notesAdapter.getNoteAt(position)
            note?.let { unarchiveNote(it) }
        }

        val trashSwipeCallback = SwipeActionCallback(
            requireContext(),
            R.drawable.ic_delete,
            R.color.delete_background,
            ItemTouchHelper.RIGHT
        ) { position ->
            val note = notesAdapter.getNoteAt(position)
            note?.let { moveToTrash(it) }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.recyclerView)
        ItemTouchHelper(trashSwipeCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.apply {
            setOnRefreshListener {
                viewModel.refreshNotes()
                isRefreshing = false
            }
        }
    }

    private fun observeNotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.archivedNotes.collectLatest { notes ->
                    notesAdapter.submitList(notes)
                    updateEmptyState(notes.isEmpty())
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateContainer.visible()
            binding.recyclerView.gone()
        } else {
            binding.emptyStateContainer.gone()
            binding.recyclerView.visible()
        }
    }

    private fun unarchiveNote(note: Note) {
        viewModel.unarchiveNote(note)
        Snackbar.make(
            binding.root,
            getString(R.string.note_unarchived),
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun moveToTrash(note: Note) {
        viewModel.moveNoteToTrash(note)
        Snackbar.make(
            binding.root,
            getString(R.string.note_moved_to_trash),
            Snackbar.LENGTH_SHORT
        ).setAction(getString(R.string.undo)) {
            viewModel.restoreNoteFromTrash(note)
        }.show()
    }

    override fun onNoteClick(note: Note) {
        val action = ArchiveFragmentDirections.actionArchiveFragmentToNoteDetailFragment(note.id)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 