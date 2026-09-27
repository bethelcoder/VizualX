package com.vizualx.app.navigation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class NavigationDestination(
    val placeName: String,
    val formattedAddress: String,
    val latitude: Double,
    val longitude: Double
)

enum class NavigationStatus {
    IDLE,
    GEOCODING,
    NAVIGATING,
    ARRIVED,
    ERROR
}

class NavigationManager(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val _destination = MutableStateFlow<NavigationDestination?>(null)
    val destination: StateFlow<NavigationDestination?> = _destination.asStateFlow()

    private val _status = MutableStateFlow(NavigationStatus.IDLE)
    val status: StateFlow<NavigationStatus> = _status.asStateFlow()

    private val _guidanceMessage = MutableStateFlow<String?>(null)
    val guidanceMessage: StateFlow<String?> = _guidanceMessage.asStateFlow()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            _currentLocation.value = location
            updateNavigationGuidance(location)
        }
    }

    private var isLocationTrackingStarted = false
    private var lastSpokenDistanceMeters: Float = -1f

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (isLocationTrackingStarted) return

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            3000L // Update location every 3 seconds
        ).setMinUpdateIntervalMillis(1500L)
         .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            isLocationTrackingStarted = true
        } catch (e: SecurityException) {
            _status.value = NavigationStatus.ERROR
            _guidanceMessage.value = "Location permission required for navigation."
        }
    }

    fun stopLocationUpdates() {
        if (isLocationTrackingStarted) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            isLocationTrackingStarted = false
        }
    }

    fun searchAndNavigateTo(
        query: String,
        onSpokenGuidance: (String) -> Unit
    ) {
        _status.value = NavigationStatus.GEOCODING
        val geocoder = Geocoder(context, Locale.getDefault())

        val cleanQuery = query.replace(Regex("(?i)^(navigate to|take me to|directions to|where is|go to)\\s+"), "").trim()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocationName(cleanQuery, 1, object : Geocoder.GeocodeListener {
                override fun onGeocode(addresses: MutableList<Address>) {
                    handleGeocodeResult(addresses, cleanQuery, onSpokenGuidance)
                }

                override fun onError(errorMessage: String?) {
                    _status.value = NavigationStatus.ERROR
                    val msg = "Could not locate '$cleanQuery'. Please check the name and try again."
                    _guidanceMessage.value = msg
                    onSpokenGuidance(msg)
                }
            })
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocationName(cleanQuery, 1)
                    handleGeocodeResult(addresses, cleanQuery, onSpokenGuidance)
                } catch (e: Exception) {
                    _status.value = NavigationStatus.ERROR
                    val msg = "Location lookup failed for '$cleanQuery'."
                    _guidanceMessage.value = msg
                    onSpokenGuidance(msg)
                }
            }
        }
    }

    private fun handleGeocodeResult(
        addresses: List<Address>?,
        query: String,
        onSpokenGuidance: (String) -> Unit
    ) {
        val firstAddress = addresses?.firstOrNull()
        if (firstAddress != null) {
            val dest = NavigationDestination(
                placeName = firstAddress.featureName ?: query.capitalize(Locale.getDefault()),
                formattedAddress = firstAddress.getAddressLine(0) ?: query,
                latitude = firstAddress.latitude,
                longitude = firstAddress.longitude
            )

            _destination.value = dest
            _status.value = NavigationStatus.NAVIGATING
            lastSpokenDistanceMeters = -1f

            val currentLoc = _currentLocation.value
            val initialSpeech = if (currentLoc != null) {
                val distMeters = calculateDistance(currentLoc, dest)
                val dirText = calculateCardinalDirection(currentLoc, dest)
                "Found ${dest.placeName}. Distance is ${formatDistance(distMeters)} $dirText. Starting real-time navigation."
            } else {
                "Found ${dest.placeName}. Starting real-time navigation."
            }

            _guidanceMessage.value = initialSpeech
            onSpokenGuidance(initialSpeech)

            // Auto-start location tracking if not active
            startLocationUpdates()
        } else {
            _status.value = NavigationStatus.ERROR
            val msg = "Sorry, I couldn't find a location matching '$query'."
            _guidanceMessage.value = msg
            onSpokenGuidance(msg)
        }
    }

    private fun updateNavigationGuidance(currentLoc: Location) {
        val dest = _destination.value ?: return
        if (_status.value != NavigationStatus.NAVIGATING) return

        val distanceMeters = calculateDistance(currentLoc, dest)
        val cardinalDirection = calculateCardinalDirection(currentLoc, dest)

        if (distanceMeters < 15f) { // Within 15 meters considered arrived
            _status.value = NavigationStatus.ARRIVED
            val arrivedMsg = "You have arrived at your destination: ${dest.placeName}."
            _guidanceMessage.value = arrivedMsg
            return
        }

        val formattedDist = formatDistance(distanceMeters)
        _guidanceMessage.value = "${dest.placeName} is $formattedDist $cardinalDirection"
    }

    fun launchExternalGoogleMapsNavigation() {
        val dest = _destination.value ?: return
        val gmmIntentUri = Uri.parse("google.navigation:q=${dest.latitude},${dest.longitude}&mode=w")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            // Fallback to web browser maps intent if app not installed
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${dest.latitude},${dest.longitude}")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(webIntent)
        }
    }

    fun stopNavigation() {
        _destination.value = null
        _status.value = NavigationStatus.IDLE
        _guidanceMessage.value = null
    }

    private fun calculateDistance(loc: Location, dest: NavigationDestination): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            loc.latitude, loc.longitude,
            dest.latitude, dest.longitude,
            results
        )
        return results[0]
    }

    private fun calculateCardinalDirection(loc: Location, dest: NavigationDestination): String {
        val results = FloatArray(2)
        Location.distanceBetween(
            loc.latitude, loc.longitude,
            dest.latitude, dest.longitude,
            results
        )
        val bearing = (results[1] + 360) % 360
        return when {
            bearing in 337.5..360.0 || bearing in 0.0..22.5 -> "North"
            bearing in 22.5..67.5 -> "North-East"
            bearing in 67.5..112.5 -> "East"
            bearing in 112.5..157.5 -> "South-East"
            bearing in 157.5..202.5 -> "South"
            bearing in 202.5..247.5 -> "South-West"
            bearing in 247.5..292.5 -> "West"
            bearing in 292.5..337.5 -> "North-West"
            else -> "Ahead"
        }
    }

    private fun formatDistance(meters: Float): String {
        return if (meters >= 1000) {
            String.format(Locale.getDefault(), "%.1f km", meters / 1000f)
        } else {
            "${meters.toInt()} meters"
        }
    }
}
