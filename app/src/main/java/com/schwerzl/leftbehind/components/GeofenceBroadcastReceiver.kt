package com.schwerzl.leftbehind.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.schwerzl.leftbehind.data.worker.CheckDevicesNearbyWorker
import com.schwerzl.leftbehind.data.worker.TRIGGERING_GEOFENCE_KEY
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Timber.d("Geofence event received!")
        val geofencingEvent = intent?.let { GeofencingEvent.fromIntent(it) } ?: return

        if (geofencingEvent.hasError()) {
            val errorMessage =
                GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Timber.e("onReceive: $errorMessage")
            return
        }
        val alertString = "Geofence Alert :" +
                " Trigger ${geofencingEvent.triggeringGeofences}" +
                " Transition ${geofencingEvent.geofenceTransition}"
        Timber.d(alertString)
        val triggeringGeoFenceIds = geofencingEvent.triggeringGeofences?.map { it.requestId } ?: emptyList()

        val inputData = workDataOf(
         TRIGGERING_GEOFENCE_KEY to triggeringGeoFenceIds.toTypedArray()
        )

        val workManager = WorkManager.getInstance(context)
        val syncRequest = OneTimeWorkRequestBuilder<CheckDevicesNearbyWorker>().build()

        workManager.enqueue(syncRequest)

    }
}