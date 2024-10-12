package com.schwerzl.leftbehind.screens

import android.location.Location
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schwerzl.leftbehind.viewmodel.DataResult
import com.schwerzl.leftbehind.viewmodel.GeoFenceViewModel
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
    val id: String,
    val name: String,
    val lat: Double,
    val long: Double,
    val radius: Float
)

@Composable
fun GeoFenceMapScreen(
    viewModel: GeoFenceViewModel = hiltViewModel(),
    onAddGeoFence: () -> Unit = {},
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

    GeofenceMapScreen(
        currentUserLocation,
        geofences,
        onAddGeoFence = onAddGeoFence,
        onRemoveGeoFence = viewModel::onTapMarker
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun GeofenceMapScreen(
    location: DataResult<Location?> = DataResult.Success(null),
    geofences: List<GeoFenceUIData> = emptyList(),
    onAddGeoFence: () -> Unit= {},
    onRemoveGeoFence: (GeoFenceUIData) -> Unit = {}
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
                    CircularOverlay(
                        mapView = map,
                        center = GeoPoint(geofence.lat, geofence.long),
                        radius = 100.0)
                )

                map.overlays.add(
                    Marker(composeMap).apply {
                        position = GeoPoint(geofence.lat, geofence.long)
                        setOnMarkerClickListener { _, _ ->
                            map.overlays.remove(this)
                            onRemoveGeoFence(geofence)
                            true
                        }
                    }
                )
            }
            map.invalidate()
        }
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddGeoFence
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        AndroidView(
            modifier = Modifier
                .padding(padding),
            factory = { context ->
                val map = MapView(context).apply {
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

                if (location is DataResult.Success) {
                    location.lst?.let {
                        map.controller.setCenter(GeoPoint(it.latitude, it.longitude))
                    }
                }
                myLocationOverlay.enableMyLocation()
                myLocationOverlay.enableFollowLocation()
                map.overlays.add(myLocationOverlay)


                map.overlays.add(MapEventsOverlay(
                    object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
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
}