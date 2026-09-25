package com.vizualx.app.ui.screens

import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.vizualx.app.camera.ActiveCameraMode
import com.vizualx.app.camera.DeviceCapabilityReport
import com.vizualx.app.context.ContextEvent
import com.vizualx.app.context.EventPriority
import com.vizualx.app.perception.models.ObjectType
import com.vizualx.app.perception.models.RelativePosition
import com.vizualx.app.speech.VoiceAssistantState
import com.vizualx.app.ui.theme.BorderSubtle
import com.vizualx.app.ui.theme.CyanPrimary
import com.vizualx.app.ui.theme.DarkBackground
import com.vizualx.app.ui.theme.DarkSurface
import com.vizualx.app.ui.theme.DarkSurfaceHighlight
import com.vizualx.app.ui.theme.HazardCritical
import com.vizualx.app.ui.theme.HazardHigh
import com.vizualx.app.ui.theme.HazardNormal
import com.vizualx.app.ui.theme.TextPrimary
import com.vizualx.app.ui.theme.TextSecondary
import com.vizualx.app.ui.theme.VioletSecondary
import kotlinx.coroutines.flow.StateFlow

@Composable
fun MainScreen(
    isAssistanceActive: Boolean,
    countdownSeconds: Int,
    onToggleAssistance: () -> Unit,
    cameraMode: ActiveCameraMode,
    assistantStateFlow: StateFlow<VoiceAssistantState>,
    lastSpokenFlow: StateFlow<String?>,
    audioDbFlow: StateFlow<Float>,
    recentEvents: List<ContextEvent>,
    capabilityReport: DeviceCapabilityReport?,
    onSimulate: (ObjectType, RelativePosition, Float, String) -> Unit,
    onVoiceQueryClick: () -> Unit,
    onPreviewViewCreated: (PreviewView) -> Unit
) {
    val assistantState by assistantStateFlow.collectAsState()
    val lastSpoken by lastSpokenFlow.collectAsState()
    val audioDb by audioDbFlow.collectAsState()
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Double tap anywhere on screen for eyes-free voice assistant trigger
                detectTapGestures(
                    onDoubleTap = { onVoiceQueryClick() }
                )
            },
        color = DarkBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
                HeaderSection(
                    assistantState = assistantState,
                    isAssistanceActive = isAssistanceActive,
                    countdownSeconds = countdownSeconds,
                    onDiagnosticsClick = { showDiagnosticsDialog = true }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Primary Ahead Camera & Audio Monitor
                AheadCameraAudioMonitor(
                    audioDb = audioDb,
                    isAssistanceActive = isAssistanceActive,
                    onPreviewViewCreated = onPreviewViewCreated
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Spoken Subtitle Banner
                SpokenBanner(lastSpoken = lastSpoken, assistantState = assistantState)

                Spacer(modifier = Modifier.height(12.dp))

                // Action Controls Row
                ActionControlsRow(
                    isAssistanceActive = isAssistanceActive,
                    onToggleAssistance = onToggleAssistance,
                    onVoiceQuery = onVoiceQueryClick
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Feasibility Spike Simulators (1-Tap Test)
                SimulationTestChips(onSimulate = onSimulate)

                Spacer(modifier = Modifier.height(12.dp))

                // Perception Event Stream Feed
                Text(
                    text = "Real-time Perception Feed",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                EventStreamList(events = recentEvents)
            }

            // Automated Countdown Overlay
            AnimatedVisibility(
                visible = countdownSeconds > 0,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.85f))
                        .border(3.dp, CyanPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$countdownSeconds",
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 48.sp, fontWeight = FontWeight.Bold),
                            color = CyanPrimary
                        )
                        Text(
                            text = "Starting...",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }

    if (showDiagnosticsDialog && capabilityReport != null) {
        DiagnosticsDialog(
            report = capabilityReport,
            onDismiss = { showDiagnosticsDialog = false }
        )
    }
}

@Composable
fun HeaderSection(
    assistantState: VoiceAssistantState,
    isAssistanceActive: Boolean,
    countdownSeconds: Int,
    onDiagnosticsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(CyanPrimary, VioletSecondary))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "VizualX Logo",
                    tint = DarkBackground,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "VizualX",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = when {
                        countdownSeconds > 0 -> "Starting in ${countdownSeconds}s..."
                        isAssistanceActive -> "Active • $assistantState"
                        else -> "Standby"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isAssistanceActive) CyanPrimary else TextSecondary
                )
            }
        }

        IconButton(
            onClick = onDiagnosticsClick,
            modifier = Modifier
                .clip(CircleShape)
                .background(DarkSurfaceHighlight)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Device Diagnostics",
                tint = CyanPrimary
            )
        }
    }
}

@Composable
fun AheadCameraAudioMonitor(
    audioDb: Float,
    isAssistanceActive: Boolean,
    onPreviewViewCreated: (PreviewView) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isAssistanceActive) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            onPreviewViewCreated(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Camera & Audio Inactive\nTap 'Start Perception' to begin",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Top Status Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "AHEAD CAM • Live Perception",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom Audio VU Level Bar
            if (isAssistanceActive) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MIC INPUT (16kHz PCM)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "${audioDb.toInt()} dB",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (audioDb > 70f) HazardCritical else CyanPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (audioDb / 90f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (audioDb > 70f) HazardCritical else CyanPrimary,
                        trackColor = BorderSubtle,
                    )
                }
            }
        }
    }
}

@Composable
fun SpokenBanner(lastSpoken: String?, assistantState: VoiceAssistantState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceHighlight),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "Voice Output",
                tint = CyanPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "LAST SPOKEN ALERT",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = lastSpoken ?: "System silent (no hazards detected)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
fun ActionControlsRow(
    isAssistanceActive: Boolean,
    onToggleAssistance: () -> Unit,
    onVoiceQuery: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onToggleAssistance,
            modifier = Modifier
                .weight(1.3f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAssistanceActive) HazardCritical else CyanPrimary,
                contentColor = DarkBackground
            )
        ) {
            Icon(
                imageVector = if (isAssistanceActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isAssistanceActive) "Pause Perception" else "Start Perception",
                fontWeight = FontWeight.Bold
            )
        }

        Button(
            onClick = onVoiceQuery,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VioletSecondary,
                contentColor = TextPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Ask AI",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SimulationTestChips(
    onSimulate: (ObjectType, RelativePosition, Float, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Feasibility Spike Simulators (1-Tap Test)",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SimulationChip(
                label = "Vehicle Alert",
                color = HazardCritical,
                onClick = { onSimulate(ObjectType.VEHICLE, RelativePosition.RIGHT, 3.5f, "Approaching Vehicle") }
            )
            SimulationChip(
                label = "Stairs Down",
                color = HazardCritical,
                onClick = { onSimulate(ObjectType.STAIRS_DOWN, RelativePosition.AHEAD, 1.8f, "Stairs Descending") }
            )
            SimulationChip(
                label = "Doorway",
                color = HazardHigh,
                onClick = { onSimulate(ObjectType.DOOR, RelativePosition.AHEAD, 5.0f, "Science Lab Entrance") }
            )
            SimulationChip(
                label = "Landmark",
                color = CyanPrimary,
                onClick = { onSimulate(ObjectType.CAMPUS_LANDMARK, RelativePosition.AHEAD, 12f, "Wits Science Stadium") }
            )
        }
    }
}

@Composable
fun SimulationChip(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceHighlight)
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = color
        )
    }
}

@Composable
fun EventStreamList(events: List<ContextEvent>) {
    if (events.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No events logged yet. Active observations will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(events) { event ->
                EventCard(event = event)
            }
        }
    }
}

@Composable
fun EventCard(event: ContextEvent) {
    val (priorityColor, priorityText) = when (event.priority) {
        EventPriority.CRITICAL -> HazardCritical to "CRITICAL"
        EventPriority.HIGH -> HazardHigh to "HIGH"
        EventPriority.NORMAL -> HazardNormal to "NORMAL"
        EventPriority.LOW -> Color.Gray to "LOW"
        EventPriority.IGNORE -> Color.DarkGray to "IGNORE"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, priorityColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.displayTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = event.displayDetail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                if (event.spokenText != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Spoke: \"${event.spokenText}\"",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanPrimary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(priorityColor.copy(alpha = 0.15f))
                    .border(1.dp, priorityColor, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = priorityText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = priorityColor
                )
            }
        }
    }
}

@Composable
fun DiagnosticsDialog(
    report: DeviceCapabilityReport,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Hardware Capability Diagnostic",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
        },
        text = {
            Column {
                Text("Device: ${report.manufacturer} ${report.deviceModel}", color = TextPrimary)
                Text("Android: ${report.androidVersion} (API ${report.apiLevel})", color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = BorderSubtle)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Concurrent Camera Support:",
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = if (report.hasConcurrentCameraSupport) "✓ YES (Dual-Stream Supported)" else "✗ NO (Single-Stream Fallback)",
                    color = if (report.hasConcurrentCameraSupport) CyanPrimary else HazardCritical,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Rear Camera ID: ${report.rearCameraId ?: "None"}", color = TextSecondary)
                Text("Front Camera ID: ${report.frontCameraId ?: "None"}", color = TextSecondary)
                Text("Total Cameras Detected: ${report.totalCameras}", color = TextSecondary)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = CyanPrimary)
            }
        },
        containerColor = DarkSurface,
        shape = RoundedCornerShape(16.dp)
    )
}
