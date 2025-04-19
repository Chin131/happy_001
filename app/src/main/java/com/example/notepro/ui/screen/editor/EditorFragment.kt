package com.example.notepro.ui.screen.editor

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.notepro.R
import com.example.notepro.databinding.FragmentEditorBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.notepro.ui.component.voice.VoiceRecorderView

@AndroidEntryPoint
class EditorFragment : Fragment() {

    private var _binding: FragmentEditorBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: EditorViewModel by viewModels()
    private val args: EditorFragmentArgs by navArgs()
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, show voice input dialog
            showVoiceInputDialog()
        } else {
            // Permission denied, show error message
            Snackbar.make(
                binding.root, 
                R.string.error_permission,
                Snackbar.LENGTH_LONG
            ).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable delayed transition
        postponeEnterTransition()
        
        // Set up shared element transition
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            duration = 300L
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(requireContext().getColor(R.color.md_theme_light_background))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditorBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set transition name for the shared element
        binding.editorRoot.transitionName = "note_card_transition"
        
        // Start transition once view is ready
        view.doOnPreDraw { startPostponedEnterTransition() }
        
        setupToolbar()
        setupSaveButton()
        setupVoiceInput()
        
        // If editing existing note, load its data
        args.noteId?.let { noteId ->
            viewModel.loadNote(noteId.toString())
        }
        
        observeViewModel()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_share -> {
                    shareNote()
                    true
                }
                R.id.action_delete -> {
                    confirmDelete()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupSaveButton() {
        binding.fabSave.setOnClickListener {
            saveNote()
        }
    }
    
    private fun setupVoiceInput() {
        binding.voiceInputButton.setOnClickListener {
            checkAudioPermissionAndShowDialog()
        }
    }
    
    private fun checkAudioPermissionAndShowDialog() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted, show dialog
                showVoiceInputDialog()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) -> {
                // Show rationale and request permission
                Snackbar.make(
                    binding.root,
                    R.string.permission_required,
                    Snackbar.LENGTH_LONG
                ).setAction(R.string.ok) {
                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }.show()
            }
            else -> {
                // No rationale needed, request permission directly
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    
    private fun showVoiceInputDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_voice_input, null)
        
        val voiceRecorderView = dialogView.findViewById<VoiceRecorderView>(R.id.voice_recorder)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.voice_input)
            .setView(dialogView)
            .setPositiveButton(R.string.done, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        
        // Set up voice recognition result listener
        voiceRecorderView.setOnTextRecognizedListener { recognizedText ->
            // Get current text and cursor position
            val currentText = binding.markdownEditor.getText()
            
            // Append or insert text
            val updatedText = if (currentText.isEmpty()) {
                recognizedText
            } else {
                "$currentText\n\n$recognizedText"
            }
            
            binding.markdownEditor.setText(updatedText)
            
            // Close dialog
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.saveState.collect { state ->
                when (state) {
                    is SaveState.Success -> {
                        Snackbar.make(
                            binding.root,
                            R.string.note_saved,
                            Snackbar.LENGTH_SHORT
                        ).show()
                        findNavController().navigateUp()
                    }
                    is SaveState.Error -> {
                        Snackbar.make(
                            binding.root,
                            state.message,
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    else -> { /* Don't handle other states */ }
                }
            }
        }
    }
    
    private fun saveNote() {
        val title = binding.titleEditText.text.toString()
        val content = binding.markdownEditor.getText()
        
        viewModel.saveNote(title, content)
    }
    
    private fun shareNote() {
        val title = binding.titleEditText.text.toString()
        val content = binding.markdownEditor.getText()
        
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
        }
        
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_note)))
    }
    
    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_note)
            .setMessage(R.string.delete_note_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteNote()
                findNavController().navigateUp()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 