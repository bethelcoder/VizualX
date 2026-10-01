package com.vizualx.app.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.vizualx.app.context.EventPriority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

enum class VoiceAssistantState {
    IDLE,
    LISTENING,
    UNDERSTANDING,
    RESPONDING,
    MONITORING
}

class SpeechManager(
    private val context: Context,
    private val onUserSpoke: ((String) -> Unit)? = null
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var speechRecognizer: SpeechRecognizer? = null

    private val _assistantState = MutableStateFlow(VoiceAssistantState.IDLE)
    val assistantState: StateFlow<VoiceAssistantState> = _assistantState.asStateFlow()

    private val _lastSpoken = MutableStateFlow<String?>(null)
    val lastSpoken: StateFlow<String?> = _lastSpoken.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.getDefault()
            }
            tts?.setSpeechRate(1.05f) // Natural conversational tempo
            tts?.setPitch(1.0f)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _assistantState.value = VoiceAssistantState.RESPONDING
                }

                override fun onDone(utteranceId: String?) {
                    _assistantState.value = VoiceAssistantState.MONITORING
                }

                override fun onError(utteranceId: String?) {
                    _assistantState.value = VoiceAssistantState.MONITORING
                }
            })
            isTtsReady = true
        }
    }

    fun speak(text: String, priority: EventPriority = EventPriority.NORMAL) {
        if (!isTtsReady || text.isBlank()) return

        // 1. Run through SpeechNormalizer to de-acronymize all-caps words, expand abbreviations,
        // merge spaced OCR letters, and remove OCR noise artifacts.
        val cleanSpeechText = SpeechNormalizer.normalizeForSpeech(text)
        if (cleanSpeechText.isBlank()) return

        val queueMode = if (priority == EventPriority.CRITICAL) {
            TextToSpeech.QUEUE_FLUSH // Immediately interrupt current speech for critical hazards
        } else {
            TextToSpeech.QUEUE_ADD
        }

        _lastSpoken.value = cleanSpeechText
        val utteranceId = UUID.randomUUID().toString()
        tts?.speak(cleanSpeechText, queueMode, null, utteranceId)
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return
        }

        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _assistantState.value = VoiceAssistantState.LISTENING
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _assistantState.value = VoiceAssistantState.UNDERSTANDING
                    }

                    override fun onError(error: Int) {
                        _assistantState.value = VoiceAssistantState.MONITORING
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        _assistantState.value = VoiceAssistantState.UNDERSTANDING
                        if (text.isNotBlank()) {
                            onUserSpoke?.invoke(text)
                        } else {
                            _assistantState.value = VoiceAssistantState.MONITORING
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _assistantState.value = VoiceAssistantState.MONITORING
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _assistantState.value = VoiceAssistantState.MONITORING
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
    }
}
