package com.schwerzl.leftbehind.data.repository

import com.schwerzl.leftbehind.data.database.UserGeoFenceDao
import com.schwerzl.leftbehind.data.database.UserGeoFenceEntity
import com.schwerzl.leftbehind.data.datasource.GeofenceManager
import com.schwerzl.leftbehind.data.datasource.LatLng
import com.schwerzl.leftbehind.models.UserGeoFence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class GeoFenceRepository @Inject constructor(
    private val geofenceManager: GeofenceManager,
    private val userGeoFenceDao: UserGeoFenceDao
) {

    suspend fun addGeofence(lat: Double, long: Double) : UserGeoFence{
        Timber.d("addGeofence: $lat, $long")
        val entity = UserGeoFenceEntity(
            latitude = lat,
            longitude = long,
            radius = 100.0f,
            name = "Test"
        )

        userGeoFenceDao.insertAll(entity)
        geofenceManager.addGeofence(
            key = entity.uid,
            location = LatLng(lat, long)
        )
        geofenceManager.registerGeofence()
        return entity.toDomain()
    }

    suspend fun removeGeofence(key: String) {
        val geofence = userGeoFenceDao.getGeoFence(uuid = key)
        geofenceManager.removeGeofence(key)
        geofenceManager.deregisterGeofence()
        userGeoFenceDao.delete(geofence)
    }

    fun getGeofences() : Flow<List<UserGeoFence>> {
        return userGeoFenceDao.getAllFlow().map { geofences -> geofences.map { it.toDomain() } }
    }

}