package com.schwerzl.leftbehind.screens

import android.graphics.Canvas
import android.graphics.Point
import android.location.Location
import android.view.MotionEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import com.schwerzl.leftbehind.viewmodel.AddGeoFenceViewModel
import com.schwerzl.leftbehind.viewmodel.DataResult
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

data class AddGeoFenceUIData(
    val lat: Double,
    val long: Double,
)


@Composable
fun GeoFenceSelectionScreen(
    viewModel: AddGeoFenceViewModel = hiltViewModel(),
    onGeoFenceAccept: (String) -> Unit
){
    val context = LocalContext.current

    val currentUserLocation by viewModel.currentUserLocation.collectAsStateWithLifecycle()

    val nextScreen by viewModel.acceptedGeoFence.collectAsStateWithLifecycle(null)


    Timber.d("Current User Location: $currentUserLocation")
    Configuration.getInstance().apply {
        userAgentValue = context.packageName
    }

    LaunchedEffect(Unit) {
        viewModel.fetchCurrentLocation()
    }

    LaunchedEffect(nextScreen) {
        nextScreen?.let {
            onGeoFenceAccept(it)
        }
    }

    AddGeoFenceScreen(
        currentUserLocation,
        selectedMapPoint = viewModel.selectedMapPoint.value,
        radius = viewModel.radius.value,
        onUserMapTap = viewModel::selectedMapPoint,
        onGeoFenceResize = {
            viewModel.radius.value = it.toDouble()
        },
        onGeoFenceAccept = viewModel::onGeoFenceAccept

    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun AddGeoFenceScreen(
    location: DataResult<Location?> = DataResult.Success(null),
    selectedMapPoint: AddGeoFenceUIData? = null,
    radius: Double = 0.0,
    onUserMapTap: (Double, Double) -> Unit = {_,_ ->},
    onUserMarkerTap: (GeoFenceUIData) -> Unit = {},
    onGeoFenceResize : (Float) -> Unit = {},
    onGeoFenceAccept: () -> Unit = {},
) {

    var composeMap by remember { mutableStateOf<MapView?>(null) }
    val mapController = remember(composeMap) { composeMap?.controller }
    val receiver by remember { mutableStateOf(
        Ok(
            onUserMapTap = onUserMapTap
        )
    ) }

    LaunchedEffect(location, mapController) {
        if(location is DataResult.Success){
            location.lst?.let {
                mapController?.zoomTo(18.0)
                mapController?.animateTo(GeoPoint(it.latitude, it.longitude))
            }
        }
    }

    LaunchedEffect(selectedMapPoint, composeMap) {
        composeMap?.let { map ->
            if(!receiver.containsOverlays() && selectedMapPoint == null) {
                val overlay = MapEventsOverlay(receiver)
                map.overlays.add(overlay)
                receiver.setOverlay(overlay, map)
            }
            selectedMapPoint?.let {
                map.overlays.add(
                    CircularOverlay(
                        mapView = map,
                        center = GeoPoint(it.lat, it.long),
                        radius = radius,
                        onResize = onGeoFenceResize
                    )
                )
                map.overlays.add(
                    Marker(composeMap).apply {
                        position = GeoPoint(it.lat, it.long)
                        setOnMarkerClickListener { _, _ ->
                            map.overlays.remove(this)
                        }
                    })
                map.invalidate()
            }
        }
    }

    BottomSheetScaffold(
        topBar = {
            NavigationBar {
                if (selectedMapPoint != null) {
                    NavigationBarItem(
                        selected = false,
                        onClick = {
                            onGeoFenceAccept()
                        },
                        icon = {
                            Icon(Icons.Filled.Check, "")
                        }
                    )
                }
            }
        },
        sheetContent = {}
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
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
                map
            }
        )
    }
}
class CircularOverlay(
    private val mapView: MapView,
    center: GeoPoint,
    private var radius: Double,
    private var onResize : (Float) -> Unit = {}
) : Overlay() {

    private val paint = android.graphics.Paint()
    private val anchorPaint = android.graphics.Paint()
    private var centerPoint: GeoPoint = center
    private var centerPx: Point = Point(0, 0)
    private var radiusPx: Float = 0f
    private val anchors = mutableListOf<Anchor>()

    private var selectedAnchor: Anchor? = null


    init {
        paint.color = Color.Red.value.toInt() // Semi-transparent red
        paint.style = android.graphics.Paint.Style.FILL
        paint.alpha = 100

        anchorPaint.color = Color.Blue.value.toInt()
        anchorPaint.style = android.graphics.Paint.Style.FILL
        anchorPaint.alpha = 100

        update(center, radius)
    }

    private fun update(center: GeoPoint, radius: Double) {
        this.centerPoint = center
        this.radius = radius

        val projection = mapView.projection
        centerPx = projection.toPixels(centerPoint, null)
        radiusPx = projection.metersToEquatorPixels(radius.toFloat())

        // Calculate anchor positions
        updateAnchors()
    }

    private fun updateAnchors() {
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
            canvas.drawCircle(anchor.x.toFloat(), anchor.y.toFloat(), 30.0f, anchorPaint)
        }
        update(centerPoint, radius)
    }

    override fun onTouchEvent(event: MotionEvent, mapView: MapView): Boolean {
        val x = event.x.toInt()
        val y = event.y.toInt()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedAnchor = anchors.find { anchor ->
                    val distance = Math.sqrt(
                        Math.pow((x - anchor.x).toDouble(), 2.0) +
                                Math.pow((y - anchor.y).toDouble(), 2.0)
                    )
                    distance <= 20 // Touch radius
                }
            }
            MotionEvent.ACTION_MOVE -> {
                selectedAnchor?.let { anchor ->
                    val newRadiusPx = Math.sqrt(
                        Math.pow((x - centerPx.x).toDouble(), 2.0) +
                                Math.pow((y - centerPx.y).toDouble(), 2.0)
                    ).toFloat()
                    radius = newRadiusPx * getMetersPerPixel()
                    onResize(radius.toFloat())
                    update(centerPoint, radius) // Update circle properties
                    updateAnchors() // Update anchor positions
                    mapView.invalidate() // Redraw the map
                }
            }
            MotionEvent.ACTION_UP -> {
                selectedAnchor = null
            }
        }

        return selectedAnchor != null || super.onTouchEvent(event, mapView)
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
private class Ok(
    val onUserMapTap: (Double, Double) -> Unit = {_,_ ->},
) : MapEventsReceiver {
    private var _map : MapView? = null
    private var _overlay: MapEventsOverlay? = null

    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
        p?.let {
            onUserMapTap(it.latitude, it.longitude)
            _map?.overlays?.remove(_overlay)
            _map = null
            _overlay = null
        }
        return true
    }

    override fun longPressHelper(p: GeoPoint?): Boolean {
        return false
    }

    fun setOverlay(
        mapOverlay: MapEventsOverlay,
        mapView: MapView
    ){
        _map = mapView
        _overlay = mapOverlay
    }

    fun containsOverlays(): Boolean {
        return _map != null && _overlay != null
    }

}