package com.schwerzl.leftbehind.datasource

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER
import com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await
import timber.log.Timber

const val CUSTOM_INTENT_GEOFENCE = "GEOFENCE-TRANSITION-INTENT-ACTION"
const val CUSTOM_REQUEST_CODE_GEOFENCE = 1001

class GeofenceManager(context: Context) {
    private val client = LocationServices.getGeofencingClient(context)
    val geofenceList = mutableMapOf<String, Geofence>()

    private val geofencingPendingIntent by lazy {
        PendingIntent.getBroadcast(
            context,
            CUSTOM_REQUEST_CODE_GEOFENCE,
            Intent(CUSTOM_INTENT_GEOFENCE),
            PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun addGeofence(
        key: String,
        location: Location,
        radiusInMeters: Float = 100.0f,
        expirationTimeInMillis: Long = 30 * 60 * 1000,
    ) {
        geofenceList[key] = createGeofence(key, location, radiusInMeters, expirationTimeInMillis)
    }

    fun removeGeofence(key: String) {
        geofenceList.remove(key)
    }

    @SuppressLint("MissingPermission")
    fun registerGeofence() {

        client.addGeofences(createGeofencingRequest(), geofencingPendingIntent)
            .addOnSuccessListener {
                Timber.d("registerGeofence: SUCCESS")
            }.addOnFailureListener { exception ->
                Timber.e("registerGeofence exception: $exception")
            }
    }

    suspend fun deregisterGeofence() = kotlin.runCatching {
        client.removeGeofences(geofencingPendingIntent).await()
        geofenceList.clear()
    }

    private fun createGeofencingRequest(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GEOFENCE_TRANSITION_ENTER)
            addGeofences(geofenceList.values.toList())
        }.build()
    }

    private fun createGeofence(
        key: String,
        location: Location,
        radiusInMeters: Float,
        expirationTimeInMillis: Long,
    ): Geofence {
        return Geofence.Builder()
            .setRequestId(key)
            .setCircularRegion(location.latitude, location.longitude, radiusInMeters)
            .setExpirationDuration(expirationTimeInMillis)
            .setTransitionTypes(GEOFENCE_TRANSITION_ENTER or GEOFENCE_TRANSITION_EXIT)
            .build()
    }

}