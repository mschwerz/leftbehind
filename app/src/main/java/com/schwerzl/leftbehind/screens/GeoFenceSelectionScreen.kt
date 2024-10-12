package com.schwerzl.leftbehind.screens

import android.graphics.Canvas
import android.graphics.Point
import android.location.Location
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
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
        viewModel::selectedMapPoint,
        viewModel::onTapMarker
    )
}

@Composable
@Preview
fun GeoFenceScreen(
    location: DataResult<Location?> = DataResult.Success(null),
    geofences: List<GeoFenceUIData> = emptyList(),
    onUserMapTap: (Double, Double) -> Unit = {_,_ ->},
    onUserMarkerTap: (GeoFenceUIData) -> Unit = {}
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
                        setOnMarkerClickListener { _, _ ->
                            onUserMarkerTap(geofence)
                            map.overlays.remove(this)
                            true
                        }
                    }
                )
                map.overlays.add(
                    CircularOverlay(
                        mapView = map,
                        center = GeoPoint(geofence.lat, geofence.long),
                        radius = 100.0
                )
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
            myLocationOverlay.enableFollowLocation()
            map.overlays.add(myLocationOverlay)


            map.overlays.add(MapEventsOverlay(
                object: MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        p?.let {
                            onUserMapTap(p.latitude, p.longitude)
                        }
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
class CircularOverlay(
    private val mapView: MapView,
    center: GeoPoint,
    private var radius: Double
) : Overlay() {

    private val paint = android.graphics.Paint()
    private val anchorPaint = android.graphics.Paint()
    private var centerPoint: GeoPoint = center
    private var centerPx: Point = Point(0, 0)
    private var radiusPx: Float = 0f
    private val anchors = mutableListOf<Anchor>()

    init {
        paint.color = Color.Red.value.toInt() // Semi-transparent red
        paint.style = android.graphics.Paint.Style.STROKE

        anchorPaint.color = Color.Blue.value.toInt()
        anchorPaint.style = android.graphics.Paint.Style.FILL

        update(center, radius)
    }

    private fun update(center: GeoPoint, radius: Double) {
        this.centerPoint = center
        this.radius = radius

        val projection = mapView.projection
        centerPx = projection.toPixels(centerPoint, null)
        radiusPx = projection.metersToEquatorPixels(radius.toFloat())

        // Calculate anchor positions
        anchors.clear()
        anchors.add(Anchor(centerPx.x, centerPx.y - radiusPx.toInt())) // Top
        anchors.add(Anchor(centerPx.x + radiusPx.toInt(), centerPx.y)) // Right
        anchors.add(Anchor(centerPx.x, centerPx.y + radiusPx.toInt())) // Bottom
        anchors.add(Anchor(centerPx.x - radiusPx.toInt(), centerPx.y)) // Left
    }

    override fun draw(canvas: Canvas, projection: Projection) {
        canvas.drawCircle(centerPx.x.toFloat(), centerPx.y.toFloat(), radiusPx, paint)

        // Draw anchors
        for (anchor in anchors) {
            canvas.drawCircle(anchor.x.toFloat(), anchor.y.toFloat(), 10f, anchorPaint)
        }
    }

    fun isAnchorTouched(x: Int, y: Int): Anchor? {
        return anchors.find { anchor ->
            val distance = Math.sqrt(
                Math.pow((x - anchor.x).toDouble(), 2.0) +
                        Math.pow((y - anchor.y).toDouble(), 2.0)
            )
            distance <= 20 // Touch radius
        }
    }
    private fun getMetersPerPixel(): Double {
        val projection = mapView.projection
        return 1.0 / projection.metersToPixels(1.0F).toDouble()
    }

    fun resize(anchor: Anchor, x: Int, y: Int) {
        val newRadiusPx = Math.sqrt(
            Math.pow((x - centerPx.x).toDouble(), 2.0) +
                    Math.pow((y - centerPx.y).toDouble(), 2.0)
        ).toFloat()

        val metersPerPixel = getMetersPerPixel()
        radius = newRadiusPx * metersPerPixel
        update(centerPoint, radius)
    }

    fun getCenter(): GeoPoint {
        return centerPoint
    }

    fun getRadius(): Double {
        return radius
    }

    data class Anchor(val x: Int, val y: Int)
}