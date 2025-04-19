package com.example.notepro.ui.screen.trash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notepro.R
import com.example.notepro.databinding.FragmentTrashBinding
import com.example.notepro.domain.model.Note
import com.example.notepro.ui.screen.home.NotesAdapter
import com.example.notepro.util.gesture.SwipeActionCallback
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TrashFragment : Fragment(), NotesAdapter.OnNoteClickListener {

    private var _binding: FragmentTrashBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TrashViewModel by viewModels()
    private lateinit var notesAdapter: NotesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrashBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
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
        binding.toolbarTrash.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(this)
        binding.recyclerViewTrash.apply {
            adapter = notesAdapter
            layoutManager = LinearLayoutManager(requireContext())
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0 || dy < 0) {
                        binding.swipeRefreshTrash.isEnabled = false
                    }
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        binding.swipeRefreshTrash.isEnabled = true
                    }
                }
            })
        }
    }

    private fun setupSwipeActions() {
        val swipeCallback = SwipeActionCallback(
            requireContext(),
            onLeftSwipe = { position ->
                val note = notesAdapter.getNoteAt(position)
                viewModel.restoreNote(note)
                showRestoreNoteSnackbar(note)
            },
            onRightSwipe = { position ->
                val note = notesAdapter.getNoteAt(position)
                viewModel.deleteNotePermanently(note)
                showDeleteNoteSnackbar(note)
            },
            leftSwipeIcon = R.drawable.ic_restore,
            rightSwipeIcon = R.drawable.ic_delete,
            leftSwipeBackground = R.color.archive_background,
            rightSwipeBackground = R.color.delete_background
        )
        swipeCallback.attachToRecyclerView(binding.recyclerViewTrash)
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshTrash.apply {
            setOnRefreshListener {
                viewModel.refreshNotes()
            }
        }
    }

    private fun observeNotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.trashedNotes.collectLatest { notes ->
                    binding.swipeRefreshTrash.isRefreshing = false
                    notesAdapter.submitList(notes)
                    updateEmptyState(notes.isEmpty())
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateTrash.root.isVisible = isEmpty
        binding.recyclerViewTrash.isVisible = !isEmpty
    }

    private fun showRestoreNoteSnackbar(note: Note) {
        Snackbar.make(
            binding.root,
            getString(R.string.note_restored),
            Snackbar.LENGTH_LONG
        ).setAction(getString(R.string.undo)) {
            viewModel.moveNoteToTrash(note)
        }.show()
    }

    private fun showDeleteNoteSnackbar(note: Note) {
        Snackbar.make(
            binding.root,
            getString(R.string.note_deleted_permanently),
            Snackbar.LENGTH_LONG
        ).show()
    }

    override fun onNoteClick(note: Note) {
        // No edit functionality in trash
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_trash, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_empty_trash -> {
                viewModel.emptyTrash()
                Snackbar.make(
                    binding.root,
                    getString(R.string.trash_emptied),
                    Snackbar.LENGTH_LONG
                ).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 