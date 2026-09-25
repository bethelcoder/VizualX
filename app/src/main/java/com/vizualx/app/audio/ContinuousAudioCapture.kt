package com.vizualx.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

class ContinuousAudioCapture(
    private val sampleRate: Int = 16000,
    private val onAudioBuffer: ((ShortArray, Float) -> Unit)? = null
) {
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _amplitudeFlow = MutableStateFlow(0f)
    val amplitudeFlow: StateFlow<Float> = _amplitudeFlow.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startCapture(): Boolean {
        if (_isCapturing.value) return true

        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBufferSize, sampleRate / 2) // 500ms buffer

        return try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                return false
            }

            audioRecord = record
            record.startRecording()
            _isCapturing.value = true

            recordingJob = scope.launch {
                val buffer = ShortArray(bufferSize)
                while (isActive && _isCapturing.value) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        val rms = calculateRms(buffer, read)
                        val db = if (rms > 0) (20 * log10(rms.toDouble())).toFloat() else 0f
                        _amplitudeFlow.value = db
                        onAudioBuffer?.invoke(buffer.copyOf(read), db)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _isCapturing.value = false
            false
        }
    }

    fun stopCapture() {
        _isCapturing.value = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        _amplitudeFlow.value = 0f
    }

    private fun calculateRms(buffer: ShortArray, read: Int): Float {
        var sum = 0.0
        for (i in 0 until read) {
            sum += buffer[i] * buffer[i]
        }
        val mean = sum / read
        return sqrt(mean).toFloat()
    }
}
