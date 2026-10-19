package com.example.campuslostfound

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.campuslostfound.ui.components.*
import com.example.campuslostfound.ui.theme.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

// ---------------------------------------------------------------------------
// MAP SCREEN — Pick Location
// Flat campus map with UniFind overlays: white rounded search bar, "use my
// location" pill, centre pin with pale-lime halo, and a white bottom sheet
// with quick chips and a Confirm Location button.
// ---------------------------------------------------------------------------

/** One campus destination: label, postal-style description and coordinates. */
private data class CampusPlace(
    val name: String,
    val address: String,
    val coords: LatLng
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBackClick: () -> Unit,
    onLocationConfirmed: (String) -> Unit = {}
) {
    val plmun = LatLng(14.3916, 121.0450)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(plmun, 17f)
    }

    var searchQuery by remember { mutableStateOf("") }

    val places = remember {
        listOf(
            CampusPlace(
                "PLMUN Main Gate",
                "PLMUN Main Gate, Security Office, PLMUN Campus",
                plmun
            ),
            CampusPlace(
                "Library",
                "Main Library, Academic Quad, PLMUN Campus",
                LatLng(14.3920, 121.0455)
            ),
            CampusPlace(
                "Canteen",
                "Student Canteen, Food Court, PLMUN Campus",
                LatLng(14.3918, 121.0458)
            ),
            CampusPlace(
                "Gymnasium",
                "Gymnasium & Sports Complex, PLMUN Campus",
                LatLng(14.3910, 121.0457)
            ),
            CampusPlace(
                "Parking Lot B",
                "Parking Lot B, North Gate, PLMUN Campus",
                LatLng(14.3923, 121.0448)
            ),
            CampusPlace(
                "Student Pavilion",
                "Lost & Found Center, Student Pavilion, PLMUN Campus",
                LatLng(14.3912, 121.0445)
            )
        )
    }

    val quickPlaces = remember {
        listOf("Library", "Canteen", "Gymnasium", "Parking Lot B")
    }

    var selectedPlace by remember { mutableStateOf("PLMUN Main Gate") }

    // The search field used to filter nothing at all — it now narrows the
    // quick-pick chips below.
    val visiblePlaces = quickPlaces.filter {
        searchQuery.isBlank() || it.contains(searchQuery.trim(), ignoreCase = true)
    }

    // Keep the map and the bottom sheet in sync: picking a chip (or searching a
    // single place) flies the camera there, so the centre pin marks the choice.
    LaunchedEffect(selectedPlace) {
        places.firstOrNull { it.name == selectedPlace }?.let { place ->
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(place.coords, 18f)
            )
        }
    }

    // ---- Location permission (blue dot + "My location") --------------------
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(currentLocationPermission(context))
    }

    /** Centres the map on the device, or on campus when no fix is available. */
    fun moveToMyLocation() {
        val target = context.lastKnownLocation() ?: plmun
        cameraPositionState.position = CameraPosition.fromLatLngZoom(target, 18f)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasLocationPermission = results.values.any { it }
        if (hasLocationPermission) moveToMyLocation()
    }

    val placeAddress = places.firstOrNull { it.name == selectedPlace }?.address
        ?: "PLMUN Main Gate, Security Office, PLMUN Campus"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UniFind.Background)
    ) {

        // ---- Campus map --------------------------------------------------
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
            uiSettings = MapUiSettings(zoomControlsEnabled = true)
        ) {
            // rememberMarkerState keeps one stable marker per place instead of
            // reallocating them on every recomposition (each keystroke, chip tap…).
            places.forEach { place ->
                Marker(
                    state = rememberMarkerState(position = place.coords),
                    title = place.name,
                    snippet = place.address
                )
            }
        }

        // ---- Centre pin with soft halo -----------------------------------
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-60).dp),
            contentAlignment = Alignment.Center
        ) {
            // Soft pale-lime halo
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(UniFind.PaleLime.copy(alpha = 0.9f))
            )
            // Dark green location pin
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = "Selected location",
                tint = UniFind.Primary,
                modifier = Modifier
                    .size(46.dp)
                    .offset(y = (-8).dp)
            )
        }

        // ---- Top overlays: back + search + "use my location" --------------
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleBackButton(onClick = onBackClick, tint = UniFind.Primary)
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                ) {
                    UniFindSearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search campus places"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // "Use my location" pill
                Row(
                    modifier = Modifier
                        .shadow(6.dp, RoundedCornerShape(999.dp))
                        .clip(RoundedCornerShape(999.dp))
                        .background(UniFind.White)
                        .clickable {
                            if (hasLocationPermission) {
                                moveToMyLocation()
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Place,
                        contentDescription = null,
                        tint = UniFind.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "My location",
                        style = MaterialTheme.typography.labelMedium,
                        color = UniFind.Text
                    )
                }
            }
        }

        // ---- Bottom sheet -------------------------------------------------
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                // max(nav, ime): sits on the gesture bar when the keyboard is
                // closed and rides above it when the place search is focused,
                // without doubling the two insets.
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime)),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = UniFind.White,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Drag handle
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(UniFind.Outline)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Selected location",
                    style = MaterialTheme.typography.labelMedium,
                    color = UniFind.TextTertiary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = selectedPlace,
                    style = MaterialTheme.typography.titleMedium,
                    color = UniFind.Text
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = UniFind.Secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = placeAddress,
                        style = MaterialTheme.typography.bodySmall,
                        color = UniFind.TextSecondary,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (visiblePlaces.isEmpty()) {
                    Text(
                        text = "No campus place matches \"$searchQuery\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = UniFind.TextTertiary
                    )
                } else {
                    FilterChipRow(
                        options = visiblePlaces,
                        selected = selectedPlace,
                        onSelect = { selectedPlace = it }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                PrimaryButton(
                    text = "Confirm Location",
                    // Hand the picked place back instead of silently dropping it.
                    onClick = { onLocationConfirmed(selectedPlace) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

private fun currentLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

/** Best-effort last known fix; null when location is off or unavailable. */
private fun Context.lastKnownLocation(): LatLng? {
    val manager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return null
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    for (provider in providers) {
        try {
            val location: Location? = manager.getLastKnownLocation(provider)
            if (location != null) return LatLng(location.latitude, location.longitude)
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the call.
        } catch (_: IllegalArgumentException) {
            // Provider disabled on this device.
        }
    }
    return null
}
