package com.vizualx.app.ui.screens

import android.location.Location
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.vizualx.app.navigation.NavigationDestination
import com.vizualx.app.navigation.NavigationManager
import com.vizualx.app.navigation.NavigationStatus
import com.vizualx.app.speech.VoiceAssistantState
import com.vizualx.app.ui.theme.BrandAccent
import com.vizualx.app.ui.theme.BrandPrimary
import com.vizualx.app.ui.theme.HazardCritical
import com.vizualx.app.ui.theme.HazardHigh
import kotlinx.coroutines.flow.StateFlow

/**
 * Pure Full-Screen Immersive UI for Assistive Vision.
 *
 * Design Architecture:
 * 1. Entire screen is a 100% edge-to-edge camera feed (or Google Maps Nav).
 * 2. Zero distracting buttons — all actions are gesture-driven and eyes-free.
 * 3. Top Tabs: "Camera Feed" and "Google Maps Nav".
 * 4. Single Tap: Triggers on-demand Assistive Reading (reads signs, plaques, documents).
 * 5. Double Tap: Activates Hands-free Voice AI Query.
 * 6. Long Press: Toggle Pause / Resume.
 */
@Composable
fun MainScreen(
    isAssistanceActive: Boolean,
    countdownSeconds: Int,
    onToggleAssistance: () -> Unit,
    assistantStateFlow: StateFlow<VoiceAssistantState>,
    lastSpokenFlow: StateFlow<String?>,
    navigationManager: NavigationManager,
    onTriggerAssistiveReading: () -> Unit,
    onVoiceQueryClick: () -> Unit,
    onPreviewViewCreated: (PreviewView) -> Unit
) {
    val assistantState by assistantStateFlow.collectAsState()
    val lastSpoken by lastSpokenFlow.collectAsState()

    val currentLocation by navigationManager.currentLocation.collectAsState()
    val destination by navigationManager.destination.collectAsState()
    val navStatus by navigationManager.status.collectAsState()
    val guidanceMessage by navigationManager.guidanceMessage.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Camera Feed, 1 = Google Maps Nav

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        // Single tap anywhere: Assistive Reading tool
                        onTriggerAssistiveReading()
                    },
                    onDoubleTap = {
                        // Double tap anywhere: Voice AI query
                        onVoiceQueryClick()
                    },
                    onLongPress = {
                        // Long press: Toggle perception
                        onToggleAssistance()
                    }
                )
            },
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // LAYER 1: Full-Screen Edge-to-Edge Surface (Camera or Map)
            if (activeTab == 0 && navStatus != NavigationStatus.NAVIGATING) {
                // Full Screen Camera View
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            onPreviewViewCreated(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Full Screen Google Maps Navigation
                FullScreenGoogleMap(
                    currentLocation = currentLocation,
                    destination = destination,
                    guidanceMessage = guidanceMessage
                )
            }

            // LAYER 2: Top Header Gradient Shadow for High Contrast
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                        )
                    )
            )

            // LAYER 3: Top Clean Switcher Tabs (Camera Feed vs Google Maps Nav)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp, start = 20.dp, end = 20.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(30.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Tab 1: Camera Feed
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (activeTab == 0) BrandPrimary else Color.Transparent)
                        .clickable { activeTab = 0 }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Camera Feed",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                // Tab 2: Google Maps Nav
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (activeTab == 1 || navStatus == NavigationStatus.NAVIGATING) BrandAccent else Color.Transparent)
                        .clickable { activeTab = 1 }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (navStatus == NavigationStatus.NAVIGATING) "Map Nav • ON" else "Google Maps Nav",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }

            // LAYER 4: Bottom Gradient Shadow for Spoken Feedback
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.90f))
                        )
                    )
            )

            // LAYER 5: Minimal Floating Spoken Feedback & Gesture Guide Pill
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Spoken Directive Pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.85f))
                        .border(1.5.dp, BrandAccent.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    when (assistantState) {
                                        VoiceAssistantState.RESPONDING -> HazardCritical
                                        VoiceAssistantState.LISTENING -> BrandAccent
                                        else -> BrandPrimary
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (assistantState) {
                                    VoiceAssistantState.LISTENING -> Icons.Default.Mic
                                    else -> Icons.AutoMirrored.Filled.VolumeUp
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (assistantState) {
                                    VoiceAssistantState.LISTENING -> "LISTENING TO VOICE QUERY..."
                                    VoiceAssistantState.UNDERSTANDING -> "ANALYZING ACTION..."
                                    else -> "LIVE ACTION DIRECTIVE"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                color = BrandAccent
                            )
                            Text(
                                text = lastSpoken ?: "Path clear. Single tap to Read text • Double tap to Ask AI.",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = Color.White,
                                maxLines = 2
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Gesture Cue for Blind Users
                Text(
                    text = "Tap: Read Text / Signs • Double Tap: Ask AI • Long Press: Pause",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    color = Color.White.copy(alpha = 0.70f)
                )
            }

            // LAYER 6: Automated Startup Countdown Overlay
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
                        .background(Color.Black.copy(alpha = 0.90f))
                        .border(3.dp, BrandAccent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$countdownSeconds",
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 50.sp, fontWeight = FontWeight.Bold),
                            color = BrandAccent
                        )
                        Text(
                            text = "Starting...",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FullScreenGoogleMap(
    currentLocation: Location?,
    destination: NavigationDestination?,
    guidanceMessage: String?
) {
    val defaultPos = LatLng(
        currentLocation?.latitude ?: -26.1929,
        currentLocation?.longitude ?: 28.0305
    )
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPos, 16f)
    }

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
                Marker(
                    state = MarkerState(position = LatLng(dest.latitude, dest.longitude)),
                    title = dest.placeName,
                    snippet = dest.formattedAddress
                )

                currentLocation?.let { loc ->
                    Polyline(
                        points = listOf(
                            LatLng(loc.latitude, loc.longitude),
                            LatLng(dest.latitude, dest.longitude)
                        ),
                        color = BrandAccent,
                        width = 8f
                    )
                }
            }
        }

        if (guidanceMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 110.dp, start = 20.dp, end = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .border(1.5.dp, BrandAccent, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = guidanceMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        }
    }
}
