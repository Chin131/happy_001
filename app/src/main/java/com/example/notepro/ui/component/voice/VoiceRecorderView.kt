package com.example.notepro.ui.component.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.example.notepro.R
import com.example.notepro.databinding.ViewVoiceRecorderBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoiceRecorderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewVoiceRecorderBinding = ViewVoiceRecorderBinding.inflate(
        LayoutInflater.from(context), this, true
    )
    
    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String? = null
    private var isRecording = false
    
    private val speechRecognizer: SpeechRecognizer by lazy {
        SpeechRecognizer.createSpeechRecognizer(context)
    }
    
    private var onTextRecognizedListener: ((String) -> Unit)? = null
    
    init {
        setupRecordButton()
        setupSpeechRecognizer()
    }
    
    private fun setupRecordButton() {
        binding.recordButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }
    }
    
    private fun setupSpeechRecognizer() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                binding.statusText.text = context.getString(R.string.listening)
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { recognizedText ->
                    onTextRecognizedListener?.invoke(recognizedText)
                    binding.statusText.text = context.getString(R.string.voice_recognition_complete)
                }
            }
            
            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> R.string.error_audio
                    SpeechRecognizer.ERROR_CLIENT -> R.string.error_client
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> R.string.error_permission
                    SpeechRecognizer.ERROR_NETWORK -> R.string.error_network
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> R.string.error_timeout
                    SpeechRecognizer.ERROR_NO_MATCH -> R.string.error_no_match
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> R.string.error_busy
                    SpeechRecognizer.ERROR_SERVER -> R.string.error_server
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> R.string.error_speech_timeout
                    else -> R.string.error_unknown
                }
                
                binding.statusText.text = context.getString(errorMessage)
                stopRecording(showError = true)
            }
            
            // Other required RecognitionListener methods with empty implementations
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }
    
    private fun startRecording() {
        // Check for permissions
        if (!hasAudioPermission()) {
            requestAudioPermission()
            return
        }
        
        try {
            // Prepare audio file
            audioFilePath = createAudioFilePath()
            
            // Set up MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFilePath)
                prepare()
                start()
            }
            
            // Update UI
            isRecording = true
            binding.recordButton.setImageResource(R.drawable.ic_stop)
            binding.statusText.text = context.getString(R.string.recording)
            binding.waveformView.visibility = View.VISIBLE
            binding.waveformView.startAnimation()
            
            // Start speech recognition
            startSpeechRecognition()
            
        } catch (e: IOException) {
            e.printStackTrace()
            binding.statusText.text = context.getString(R.string.error_recording)
            stopRecording(showError = true)
        }
    }
    
    private fun stopRecording(showError: Boolean = false) {
        // Stop media recorder
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
        } catch (e: Exception) {
            // Ignore error when stopping
            e.printStackTrace()
        }
        
        // Stop speech recognizer
        speechRecognizer.stopListening()
        
        // Update UI
        isRecording = false
        binding.recordButton.setImageResource(R.drawable.ic_mic)
        binding.waveformView.stopAnimation()
        binding.waveformView.visibility = View.INVISIBLE
        
        if (!showError) {
            binding.statusText.text = context.getString(R.string.processing)
            
            // If not stopped due to error, perform conversion
            if (audioFilePath != null) {
                convertAudioToText(audioFilePath!!)
            }
        }
    }
    
    private fun startSpeechRecognition() {
        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        try {
            speechRecognizer.startListening(recognizerIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            binding.statusText.text = context.getString(R.string.error_recognition)
        }
    }
    
    private fun convertAudioToText(audioFilePath: String) {
        // In a real app, you might use a more advanced speech recognition service or API
        // Here we're just using the results from Android's built-in recognition
        binding.statusText.text = context.getString(R.string.voice_recognition_complete)
    }
    
    private fun createAudioFilePath(): String {
        val audioDir = File(context.filesDir, "audio")
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "AUDIO_$timestamp.3gp"
        return File(audioDir, fileName).absolutePath
    }
    
    private fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestAudioPermission() {
        // In Fragment or Activity, request permission
        binding.statusText.text = context.getString(R.string.permission_required)
    }
    
    fun setOnTextRecognizedListener(listener: (String) -> Unit) {
        onTextRecognizedListener = listener
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        
        // Release resources
        mediaRecorder?.release()
        mediaRecorder = null
        
        speechRecognizer.destroy()
    }
} 