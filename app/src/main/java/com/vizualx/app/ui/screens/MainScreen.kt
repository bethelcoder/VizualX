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
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.vizualx.app.navigation.NavigationManager
import com.vizualx.app.navigation.NavigationStatus
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.vizualx.app.ui.theme.BorderMedium
import com.vizualx.app.ui.theme.BorderSubtle
import com.vizualx.app.ui.theme.BrandAccent
import com.vizualx.app.ui.theme.BrandAccentLight
import com.vizualx.app.ui.theme.BrandPrimary
import com.vizualx.app.ui.theme.HazardCritical
import com.vizualx.app.ui.theme.HazardCriticalBg
import com.vizualx.app.ui.theme.HazardHigh
import com.vizualx.app.ui.theme.HazardHighBg
import com.vizualx.app.ui.theme.HazardLow
import com.vizualx.app.ui.theme.HazardLowBg
import com.vizualx.app.ui.theme.HazardNormal
import com.vizualx.app.ui.theme.HazardNormalBg
import com.vizualx.app.ui.theme.LightBackground
import com.vizualx.app.ui.theme.LightSurface
import com.vizualx.app.ui.theme.LightSurfaceVariant
import com.vizualx.app.ui.theme.TextPrimary
import com.vizualx.app.ui.theme.TextSecondary
import com.vizualx.app.ui.theme.TextTertiary
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
    navigationManager: NavigationManager,
    onSimulate: (ObjectType, RelativePosition, Float, String) -> Unit,
    onVoiceQueryClick: () -> Unit,
    onPreviewViewCreated: (PreviewView) -> Unit
) {
    val assistantState by assistantStateFlow.collectAsState()
    val lastSpoken by lastSpokenFlow.collectAsState()
    val audioDb by audioDbFlow.collectAsState()

    val currentLocation by navigationManager.currentLocation.collectAsState()
    val destination by navigationManager.destination.collectAsState()
    val navStatus by navigationManager.status.collectAsState()
    val guidanceMessage by navigationManager.guidanceMessage.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Camera, 1 = Navigation Map
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
        color = LightBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header Bar
                HeaderSection(
                    assistantState = assistantState,
                    isAssistanceActive = isAssistanceActive,
                    countdownSeconds = countdownSeconds,
                    onDiagnosticsClick = { showDiagnosticsDialog = true }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // View Switcher Tabs (Camera vs Google Maps Navigation)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(LightSurfaceVariant)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTab == 0) LightSurface else Color.Transparent)
                            .clickable { activeTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (activeTab == 0) BrandPrimary else TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Camera Feed",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (activeTab == 0) BrandPrimary else TextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activeTab == 1 || navStatus == NavigationStatus.NAVIGATING) BrandAccentLight else Color.Transparent)
                            .clickable { activeTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = BrandAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (navStatus == NavigationStatus.NAVIGATING) "Live Map Nav • ON" else "Google Maps Nav",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = BrandAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (activeTab == 0 && navStatus != NavigationStatus.NAVIGATING) {
                    // Primary Ahead Camera & Audio Monitor
                    AheadCameraAudioMonitor(
                        audioDb = audioDb,
                        isAssistanceActive = isAssistanceActive,
                        onPreviewViewCreated = onPreviewViewCreated
                    )
                } else {
                    // Google Maps Real-Time Navigation View
                    NavigationMapCard(
                        navigationManager = navigationManager,
                        currentLocation = currentLocation,
                        destination = destination,
                        navStatus = navStatus,
                        guidanceMessage = guidanceMessage
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Spoken Subtitle Banner
                SpokenBanner(lastSpoken = lastSpoken, assistantState = assistantState)

                Spacer(modifier = Modifier.height(10.dp))

                // Action Controls Row
                ActionControlsRow(
                    isAssistanceActive = isAssistanceActive,
                    onToggleAssistance = onToggleAssistance,
                    onVoiceQuery = onVoiceQueryClick
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Feasibility Spike Simulators (1-Tap Test)
                SimulationTestChips(onSimulate = onSimulate)

                Spacer(modifier = Modifier.height(10.dp))

                // Perception Event Stream Feed
                Text(
                    text = "Real-time Perception Feed",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(LightSurface)
                        .border(3.dp, BrandAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$countdownSeconds",
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 44.sp, fontWeight = FontWeight.Bold),
                            color = BrandAccent
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
                    .clip(RoundedCornerShape(10.dp))
                    .background(BrandPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "VizualX Logo",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "VizualX",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                    color = TextPrimary
                )
                Text(
                    text = when {
                        countdownSeconds > 0 -> "Starting in ${countdownSeconds}s..."
                        isAssistanceActive -> "Active • $assistantState"
                        else -> "Standby"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isAssistanceActive) BrandAccent else TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        IconButton(
            onClick = onDiagnosticsClick,
            modifier = Modifier
                .clip(CircleShape)
                .background(LightSurfaceVariant)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Device Diagnostics",
                tint = BrandPrimary
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
            .height(190.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LightSurface),
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
                        .background(LightSurfaceVariant),
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
                    .clip(RoundedCornerShape(6.dp))
                    .background(LightSurface.copy(alpha = 0.92f))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "AHEAD CAM • Live Perception",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom Audio VU Level Bar
            if (isAssistanceActive) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .background(LightSurface.copy(alpha = 0.92f))
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
                            color = if (audioDb > 70f) HazardCritical else BrandAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (audioDb / 90f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (audioDb > 70f) HazardCritical else BrandAccent,
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
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(BrandAccentLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Voice Output",
                    tint = BrandAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "LAST SPOKEN ALERT",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextTertiary,
                    fontWeight = FontWeight.SemiBold
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
                containerColor = if (isAssistanceActive) HazardCritical else BrandPrimary,
                contentColor = Color.White
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
                containerColor = BrandAccent,
                contentColor = Color.White
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
                bgColor = HazardCriticalBg,
                onClick = { onSimulate(ObjectType.VEHICLE, RelativePosition.RIGHT, 3.5f, "Approaching Vehicle") }
            )
            SimulationChip(
                label = "Stairs Down",
                color = HazardCritical,
                bgColor = HazardCriticalBg,
                onClick = { onSimulate(ObjectType.STAIRS_DOWN, RelativePosition.AHEAD, 1.8f, "Stairs Descending") }
            )
            SimulationChip(
                label = "Doorway",
                color = HazardHigh,
                bgColor = HazardHighBg,
                onClick = { onSimulate(ObjectType.DOOR, RelativePosition.AHEAD, 5.0f, "Science Lab Entrance") }
            )
            SimulationChip(
                label = "Landmark",
                color = BrandAccent,
                bgColor = BrandAccentLight,
                onClick = { onSimulate(ObjectType.CAMPUS_LANDMARK, RelativePosition.AHEAD, 12f, "Wits Science Stadium") }
            )
        }
    }
}

@Composable
fun SimulationChip(
    label: String,
    color: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
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
    val (priorityColor, priorityBg, priorityText) = when (event.priority) {
        EventPriority.CRITICAL -> Triple(HazardCritical, HazardCriticalBg, "CRITICAL")
        EventPriority.HIGH -> Triple(HazardHigh, HazardHighBg, "HIGH")
        EventPriority.NORMAL -> Triple(HazardNormal, HazardNormalBg, "NORMAL")
        EventPriority.LOW -> Triple(HazardLow, HazardLowBg, "LOW")
        EventPriority.IGNORE -> Triple(TextTertiary, LightSurfaceVariant, "IGNORE")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
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
                        color = BrandAccent,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(priorityBg)
                    .border(1.dp, priorityColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
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
                    color = if (report.hasConcurrentCameraSupport) BrandAccent else HazardCritical,
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
                Text("Close", color = BrandAccent, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = LightSurface,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun NavigationMapCard(
    navigationManager: NavigationManager,
    currentLocation: android.location.Location?,
    destination: com.vizualx.app.navigation.NavigationDestination?,
    navStatus: NavigationStatus,
    guidanceMessage: String?
) {
    val initialLatLng = when {
        destination != null -> LatLng(destination.latitude, destination.longitude)
        currentLocation != null -> LatLng(currentLocation.latitude, currentLocation.longitude)
        else -> LatLng(-26.1929, 28.0305) // Default Johannesburg
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng, 15f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                currentLocation?.let { loc ->
                    Marker(
                        state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                        title = "Your Location"
                    )
                }

                destination?.let { dest ->
                    val destLatLng = LatLng(dest.latitude, dest.longitude)
                    Marker(
                        state = MarkerState(position = destLatLng),
                        title = dest.placeName,
                        snippet = dest.formattedAddress
                    )

                    currentLocation?.let { loc ->
                        Polyline(
                            points = listOf(
                                LatLng(loc.latitude, loc.longitude),
                                destLatLng
                            ),
                            color = BrandAccent,
                            width = 10f
                        )
                    }
                }
            }

            // Top Guidance Overlay Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .background(LightSurface.copy(alpha = 0.95f))
                    .border(1.dp, BorderSubtle)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (navStatus) {
                                NavigationStatus.NAVIGATING -> "REAL-TIME NAVIGATION"
                                NavigationStatus.GEOCODING -> "LOCATING DESTINATION..."
                                NavigationStatus.ARRIVED -> "DESTINATION REACHED"
                                NavigationStatus.ERROR -> "NAVIGATION ERROR"
                                NavigationStatus.IDLE -> "GOOGLE MAPS NAV"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (navStatus == NavigationStatus.NAVIGATING) BrandAccent else TextTertiary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = guidanceMessage ?: "Say 'Navigate to [Location]' to start live directions.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextPrimary
                        )
                    }

                    if (destination != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { navigationManager.launchExternalGoogleMapsNavigation() },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(BrandAccent)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = "Turn-by-Turn Voice Nav",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { navigationManager.stopNavigation() },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(HazardCritical)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Navigation",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
