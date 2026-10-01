package com.vizualx.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.vizualx.app.accessibility.HapticFeedbackManager
import com.vizualx.app.ai.AIOrchestrator
import com.vizualx.app.ai.AIQueryResult
import com.vizualx.app.audio.ContinuousAudioCapture
import com.vizualx.app.camera.ActiveCameraMode
import com.vizualx.app.camera.CameraCapabilityDetector
import com.vizualx.app.camera.CameraStreamManager
import com.vizualx.app.camera.DeviceCapabilityReport
import com.vizualx.app.context.ContextEvent
import com.vizualx.app.context.EventPriority
import com.vizualx.app.navigation.NavigationManager
import com.vizualx.app.perception.PerceptionEngine
import com.vizualx.app.perception.models.ObservationSource
import com.vizualx.app.services.ForegroundPerceptionService
import com.vizualx.app.speech.SpeechManager
import com.vizualx.app.ui.screens.MainScreen
import com.vizualx.app.ui.theme.VizualXTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private lateinit var capabilityDetector: CameraCapabilityDetector
    private lateinit var cameraStreamManager: CameraStreamManager
    private lateinit var audioCapture: ContinuousAudioCapture
    private lateinit var perceptionEngine: PerceptionEngine
    private lateinit var speechManager: SpeechManager
    private lateinit var hapticManager: HapticFeedbackManager
    private lateinit var aiOrchestrator: AIOrchestrator
    private lateinit var navigationManager: NavigationManager

    private var capabilityReport: DeviceCapabilityReport? by mutableStateOf(null)
    private var isAssistanceActive by mutableStateOf(false)
    private var countdownSeconds by mutableIntStateOf(-1) // -1 = not counting down
    private var activeCameraMode by mutableStateOf(ActiveCameraMode.REAR_AHEAD)
    private val recentEvents = mutableStateListOf<ContextEvent>()

    private var activePreviewView: PreviewView? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] == true
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true

        if (cameraGranted && audioGranted) {
            startAutomatedStartupSequence()
        } else {
            Toast.makeText(this, "Permissions are required for VizualX", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize core engines
        capabilityDetector = CameraCapabilityDetector(this)
        capabilityReport = capabilityDetector.detectCapabilities()

        perceptionEngine = PerceptionEngine(context = this)
        val geminiClient = com.vizualx.app.ai.GeminiApiClient(this)
        aiOrchestrator = AIOrchestrator(
            worldState = perceptionEngine.worldState,
            geminiApiClient = geminiClient,
            localLlmBrain = perceptionEngine.localLlmBrain
        )
        hapticManager = HapticFeedbackManager(this)
        navigationManager = NavigationManager(this)

        speechManager = SpeechManager(this) { userQuery ->
            lifecycleScope.launch {
                val currentFrame = perceptionEngine.latestBitmap
                when (val result = aiOrchestrator.processQuery(userQuery, currentFrame)) {
                    is AIQueryResult.TriggerAssistiveReading -> {
                        speechManager.speak("Reading text in front of you.", EventPriority.HIGH)
                        val text = perceptionEngine.readAloudCurrentView()
                        speechManager.speak(text, EventPriority.CRITICAL)
                    }
                    is AIQueryResult.TextResponse -> {
                        speechManager.speak(result.text, EventPriority.HIGH)
                    }
                    is AIQueryResult.NavigationRequest -> {
                        speechManager.speak("Searching location for ${result.destinationQuery}", EventPriority.HIGH)
                        navigationManager.searchAndNavigateTo(result.destinationQuery) { spokenGuidance ->
                            speechManager.speak(spokenGuidance, EventPriority.HIGH)
                        }
                    }
                }
            }
        }

        audioCapture = ContinuousAudioCapture { buffer, db ->
            perceptionEngine.processAudioBuffer(buffer, db)
        }

        cameraStreamManager = CameraStreamManager(this, this)
        cameraStreamManager.initialize {
            // Once camera is ready, check permissions and auto-start
            checkAndAutoStart()
        }

        // Collect perception events
        lifecycleScope.launch {
            perceptionEngine.eventFlow.collect { event ->
                recentEvents.add(0, event)
                if (recentEvents.size > 50) recentEvents.removeLast()

                if (event.spokenText != null) {
                    speechManager.speak(event.spokenText, event.priority)
                }
                hapticManager.triggerFeedback(event.priority)
            }
        }

        setContent {
            VizualXTheme {
                MainScreen(
                    isAssistanceActive = isAssistanceActive,
                    countdownSeconds = countdownSeconds,
                    onToggleAssistance = {
                        if (isAssistanceActive) {
                            stopPerceptionPipeline()
                        } else {
                            startAutomatedStartupSequence()
                        }
                    },
                    assistantStateFlow = speechManager.assistantState,
                    lastSpokenFlow = speechManager.lastSpoken,
                    navigationManager = navigationManager,
                    onTriggerAssistiveReading = {
                        lifecycleScope.launch {
                            speechManager.speak("Reading text in front of you.", EventPriority.HIGH)
                            val text = perceptionEngine.readAloudCurrentView()
                            speechManager.speak(text, EventPriority.CRITICAL)
                        }
                    },
                    onVoiceQueryClick = {
                        speechManager.startListening()
                    },
                    onPreviewViewCreated = { previewView ->
                        activePreviewView = previewView
                        if (isAssistanceActive) {
                            bindCameraToPreview(previewView)
                        }
                    }
                )
            }
        }
    }

    private fun checkAndAutoStart() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            startAutomatedStartupSequence()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun getTimeGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    private fun startAutomatedStartupSequence() {
        if (isAssistanceActive) return

        lifecycleScope.launch {
            val greeting = getTimeGreeting()
            countdownSeconds = 3
            speechManager.speak("$greeting. VizualX starting in three... two... one...", EventPriority.CRITICAL)

            // Visual and audio countdown
            delay(1000)
            countdownSeconds = 2
            delay(1000)
            countdownSeconds = 1
            delay(1000)
            countdownSeconds = 0

            // Activate perception engine
            startPerceptionPipeline()
            countdownSeconds = -1
        }
    }

    private fun startPerceptionPipeline() {
        isAssistanceActive = true
        ForegroundPerceptionService.startService(this)
        audioCapture.startCapture()

        activePreviewView?.let { bindCameraToPreview(it) }
        speechManager.speak("Perception active. Monitoring your environment.", EventPriority.HIGH)
    }

    private fun stopPerceptionPipeline() {
        isAssistanceActive = false
        cameraStreamManager.stop()
        audioCapture.stopCapture()
        navigationManager.stopLocationUpdates()
        ForegroundPerceptionService.stopService(this)
        speechManager.speak("VizualX paused.", EventPriority.NORMAL)
    }

    private fun bindCameraToPreview(previewView: PreviewView) {
        cameraStreamManager.startSingleCamera(
            previewView = previewView,
            lensFacing = CameraSelector.LENS_FACING_BACK,
            onFrameAnalyzed = { frame ->
                perceptionEngine.processCameraFrame(frame, ObservationSource.REAR_CAMERA)
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraStreamManager.shutdown()
        audioCapture.stopCapture()
        navigationManager.stopLocationUpdates()
        perceptionEngine.close()
        speechManager.shutdown()
    }
}
