package com.schwerzl.leftbehind.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.schwerzl.leftbehind.data.database.UserGeoFenceDao
import com.schwerzl.leftbehind.data.datasource.GeofenceManager
import com.schwerzl.leftbehind.data.datasource.LatLng
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class GeoFenceSyncWorker @AssistedInject constructor(
    private val userGeoFence: UserGeoFenceDao,
    private val geofenceManager: GeofenceManager,
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Timber.d("Running GeoFence Sync Worker")
        val currentGeoFences = userGeoFence.getAll()
        for(geofence in currentGeoFences){
            geofenceManager.addGeofence(
                key = geofence.uid,
                location = LatLng(geofence.latitude, geofence.longitude)
            )
            geofenceManager.registerGeofence()
        }
       return Result.success()
    }

}