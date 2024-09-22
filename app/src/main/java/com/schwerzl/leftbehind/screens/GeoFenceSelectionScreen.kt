package com.schwerzl.leftbehind.screens

import android.annotation.SuppressLint
import android.location.Location
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schwerzl.leftbehind.datasource.GeofenceManager
import com.schwerzl.leftbehind.viewmodel.DataResult
import com.schwerzl.leftbehind.viewmodel.GeoFenceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import timber.log.Timber

data class GeoFenceUIData(
    val name: String,
    val lat: Double,
    val long: Double,
    val radius: Float
)


@Composable
fun GeoFenceSelectionScreen(
    viewModel: GeoFenceViewModel = hiltViewModel()
){
    val context = LocalContext.current

    val currentUserLocation by viewModel.currentUserLocation.collectAsStateWithLifecycle()
    val geofences by viewModel.userGeoFences.collectAsStateWithLifecycle()

    Timber.d("Current User Location: $currentUserLocation")
    Configuration.getInstance().apply {
        userAgentValue = context.packageName
    }

    LaunchedEffect(Unit) {
        viewModel.fetchCurrentLocation()
    }

    GeoFenceScreen(
        currentUserLocation,
        geofences,
        viewModel::selectedMapPoint
    )
}

@Composable
@Preview
fun GeoFenceScreen(
    location: DataResult<Location?> = DataResult.Success(null),
    geofences: List<GeoFenceUIData> = emptyList(),
    onUserMapTap: (Double, Double) -> Unit = {_,_ ->},
) {

    var composeMap by remember { mutableStateOf<MapView?>(null) }

    val mapController = remember(composeMap) { composeMap?.controller }
    LaunchedEffect(location, mapController) {
        if(location is DataResult.Success){
            location.lst?.let {
                mapController?.zoomTo(18.0)
                mapController?.animateTo(GeoPoint(it.latitude, it.longitude))
            }
        }
    }

    LaunchedEffect(geofences, composeMap) {
        composeMap?.let { map ->
            for(geofence in geofences) {
                map.overlays.add(
                    Marker(composeMap).apply {
                        position = GeoPoint(geofence.lat, geofence.long)
                    }
                )
            }
            map.invalidate()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val map = MapView(context).apply{
                setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
                setMultiTouchControls(true)
                minZoomLevel = 5.0
            }
            composeMap = map

            val myLocationOverlay = MyLocationNewOverlay(map)

            map.controller.apply {
                setZoom(6.0)
                setCenter(GeoPoint(0.0, 0.0))
            }

            if(location is DataResult.Success){
                location.lst?.let {
                    map.controller.setCenter(GeoPoint(it.latitude, it.longitude))
                }
            }
            myLocationOverlay.enableMyLocation()
            map.overlays.add(myLocationOverlay)

            map.overlays.add(MapEventsOverlay(
                object: MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        Timber.d("Tapped this point: $p")
                        map.overlays.add(
                            Marker(map).apply {
                                position = p
                                isDraggable = true
                            }
                        )

//                        p?.let {
//                            onUserMapTap(p.latitude, p.longitude)
//                        }
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint?): Boolean {
                        Timber.d("Long pressed this point: $p")
                        return true
                    }
                }
            ))
            map
        }
    )
}




@SuppressLint("InlinedApi")

@Composable
fun GeofencingScreen() {
    GeofencingControls()
}

@Composable
private fun GeofencingControls() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val geofenceManager = remember { GeofenceManager(context) }
    var geofenceTransitionEventInfo by remember {
        mutableStateOf("")
    }

    DisposableEffect(LocalLifecycleOwner.current) {
        onDispose {
            scope.launch(Dispatchers.IO) {
                geofenceManager.deregisterGeofence()
            }
        }
    }

    // Register a local broadcast to receive activity transition updates
//    GeofenceBroadcastReceiver(systemAction = CUSTOM_INTENT_GEOFENCE) { event ->
//        geofenceTransitionEventInfo = event
//    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GeofenceList(geofenceManager)
        Button(
            onClick = {
                if (geofenceManager.geofenceList.isNotEmpty()) {
                    geofenceManager.registerGeofence()
                } else {
                    Toast.makeText(
                        context,
                        "Please add at least one geofence to monitor",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
        ) {
            Text(text = "Register Geofences")
        }

        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    geofenceManager.deregisterGeofence()
                }
            },
        ) {
            Text(text = "Deregister Geofences")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = geofenceTransitionEventInfo)
    }
}

@Composable
fun GeofenceList(geofenceManager: GeofenceManager) {
    // for geofences
    val checkedGeoFence1 = remember { mutableStateOf(false) }
    val checkedGeoFence2 = remember { mutableStateOf(false) }
    val checkedGeoFence3 = remember { mutableStateOf(false) }

    Text(text = "Available Geofence")
    Row(
        Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checkedGeoFence1.value,
            onCheckedChange = { checked ->
                if (checked) {
                    geofenceManager.addGeofence(
                        "statue_of_liberty",
                        location = Location("").apply {
                            latitude = 40.689403968838015
                            longitude = -74.04453795094359
                        },
                    )
                } else {
                    geofenceManager.removeGeofence("statue_of_libery")
                }
                checkedGeoFence1.value = checked
            },
        )
        Text(text = "Statue of Liberty")
    }
    Row(
        Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checkedGeoFence2.value,
            onCheckedChange = { checked ->
                if (checked) {
                    geofenceManager.addGeofence(
                        "eiffel_tower",
                        location = Location("").apply {
                            latitude = 48.85850
                            longitude = 2.29455
                        },
                    )
                } else {
                    geofenceManager.removeGeofence("eiffel_tower")
                }
                checkedGeoFence2.value = checked
            },
        )
        Text(text = "Eiffel Tower")
    }
    Row(
        Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checkedGeoFence3.value,
            onCheckedChange = { checked ->
                if (checked) {
                    geofenceManager.addGeofence(
                        "vatican_city",
                        location = Location("").apply {
                            latitude = 41.90238
                            longitude = 12.45398
                        },
                    )
                } else {
                    geofenceManager.removeGeofence("vatican_city")
                }
                checkedGeoFence3.value = checked
            },
        )
        Text(text = "Vatican City")
    }
}